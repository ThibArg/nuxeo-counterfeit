/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
/*
 * WARNING WARNING WARNING
 * 
 *  About all is hard coded, and/orcopy/paste form other plug-ins
 *  The goals is just to quickly create some data, not to build the state-of-the-art example :->
 */

package org.counterfeit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Create n Affaire/AffairePrestExt (1/2 of each), with pseudo-random values.
 * "pseudo" because we make sure some brands ("marques") and some
 * "contrevenants" are more involved than others, so showing stats looks cool
 * (not an eve repartition between each)
 * 
 * Hopefully, reading the code will be clear enough (yes, this is what you say
 * when you don't write a doc)
 * 
 * Something VERY IMPORTANT: The values must be the same as the one used in the
 * Studio project.
 */
@Operation(id = CreateDemoDataOp.ID, category = Constants.CAT_SERVICES, label = "CreateDemoDataOp", description = "")
public class CreateDemoDataOp {

    public static final String ID = "CreateDemoDataOp";

    private static final Log log = LogFactory.getLog(CreateDemoDataOp.class);

    protected static final int COMMIT_MODULO = 50;

    protected static final String[] ANTI_CONTREFACON_USERS = { "utilisateur1",
            "utilisateur1", "utilisateur1", "utilisateur1", "utilisateur2" };

    protected static final int ANTI_CONTREFACON_USERS_MAX = ANTI_CONTREFACON_USERS.length - 1;

    protected static final String[] AUTORITES = { "Customs", "Customs",
            "Customs", "Customs", "Police", "Police", "DGCCRF", "DGCCRF", "FDA" };

    // From the "AuthorityInvolved" vocabulary
    protected static final int AUTORITES_MAX = AUTORITES.length - 1;

    protected static final String[] MAIN_MARQUES = { "Dom Pérignon",
            "Tag Heuer", "Tag Heuer", "Tag Heuer", "Louis Vuitton",
            "Louis Vuitton" };

    protected static final int MAIN_MARQUES_MAX = MAIN_MARQUES.length - 1;

    protected static final String[] OTHER_MARQUES = { "Chaumet", "Guerlain",
            "Guerlain", "Guerlain", "Fred", "Kenzo", "Givenchy", "Henessy" };

    protected static final int OTHER_MARQUES_MAX = OTHER_MARQUES.length - 1;

    protected static final int[] QUANTITIES = { 10, 20, 50, 100, 250, 500, 750,
            1000, 2000 };

    protected static final int QUANTITIES_MAX = QUANTITIES.length - 1;

    // Cf "AffaireType" vocabulary
    protected static final String[] TYPE_AFFAIRE = { "PC", "PC", "MJ", "MJ",
            "MJ", "MJ", "CDC" };

    protected static final int TYPE_AFFAIRE_MAX = TYPE_AFFAIRE.length - 1;

    // Cf "OperationType" vocabulary
    protected static final String[] TYPE_OPERATION = { "Douane", "Douane",
            "Douane", "Douane", "Investigation", "Investigation",
            "Investigation", "Saisie Police", "Raid", "Investigation",
            "Training" };

    protected static final int TYPE_OPERATION_MAX = TYPE_OPERATION.length - 1;

    protected static final String[] FROM_COUNTRIES = { "BW", "TD", "CN", "CN",
            "CN", "AL", "KH" };

    protected static final double[][] FROM_COUNTRIES_GEO = {
            { 24.684866, -22.328474 }, { 100.50176510, 13.7563309 },
            { 121.473701, 31.230416 }, { 121.473701, 31.230416 },
            { 121.473701, 31.230416 }, { 20.168330, 41.153332 },
            { 104.8921668, 11.5448729 } };

    protected static final int FROM_COUNTRIES_MAX = FROM_COUNTRIES.length - 1;

    protected static final String[] TO_COUNTRIES = { "DE", "ES", "CH", "FR",
            "US", "CN" };

    protected static final double[][] TO_COUNTRIES_GEO = {
            { 13.404953, 52.520006 }, { -3.7037901, 40.4167754 },
            { 6.142296, 46.1983922 }, { 5.369779, 43.296482 },
            { -118.2436849, 34.0522342 }, { 116.407394, 39.904211 } };

    protected static final int TO_COUNTRIES_MAX = TO_COUNTRIES.length - 1;

    protected long todayAsMS;

    protected int count = 0;

    protected String parentPath;

    protected DateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected DateFormat yyyy = new SimpleDateFormat("yyyy");

    protected Calendar today = Calendar.getInstance();

    protected String[] contrevenants;

    protected int contrevenants_max;

    protected String theUser;

    @Context
    protected CoreSession session;

    @Param(name = "howMany", required = false, values = { "2000" })
    protected long howMany;

    @OperationMethod
    public void run() throws DocumentException, LifeCycleException {

        log.warn("Creating " + howMany
                + " documents Affaire/AffairePrestExt...");

        todayAsMS = Calendar.getInstance().getTimeInMillis();

        setupContrevenants();

        // Find the "Affaires" folder
        setUpAffairesPath();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        howMany = howMany <= 0 ? 2000 : howMany;
        count = 0;
        for (int i = 1; i <= howMany; i++) {
            createOneAffaire();

            count += 1;
            if ((count % 50) == 0) {
                log.warn("Created: " + count + "/" + howMany);
            }
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("Creation of " + howMany
                + " documents Affaire/AffairePrestExt done.");
    }

    protected void createOneAffaire() throws DocumentException,
            LifeCycleException {

        DocumentModel doc;

        Calendar created = RandomValues.buildDate(null, 0, 360, true);
        boolean isAffaireDouane = RandomValues.randomInt(1, 2) == 1;
        boolean isAffairePrestExt = isAffaireDouane ? false : true;
        String typeAffaire, origineAffaire, docType;
        if (isAffaireDouane) {
            origineAffaire = "Douane";
            theUser = "douanier";
            docType = "Affaire";
        } else if (isAffairePrestExt) {
            origineAffaire = "Prestataire Externe";
            theUser = "prestext1";
            docType = "AffairePrestExt";
        } else {
            throw new ClientException(
                    "Not douanier, nor prest. ext => impossible");
        }
        typeAffaire = TYPE_AFFAIRE[RandomValues.randomInt(0, TYPE_AFFAIRE_MAX)];

        // Build the title as in Studio config
        // @{Document["affaire:type_affaire"]}.@{yearStr}.@{String.format("%04d",
        // Integer.parseInt(Fn.getNextId(yearStr +"-AFFAIRE")) )}
        String yearStr = yyyy.format(created.getTime());
        UIDSequencer svc = Framework.getService(UIDSequencer.class);
        int next = svc.getNext(yearStr + "-AFFAIRE");
        String title = typeAffaire + "." + yearStr + "."
                + String.format("%04d", next);

        // Create the model
        doc = session.createDocumentModel(parentPath, title, docType);

        // Setup values
        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("affaire:origine_affaire", origineAffaire);

        // =========================================== dublincore
        doc.setPropertyValue("dc:creator", theUser);
        doc.setPropertyValue("dc:lastContributor", theUser);
        doc.setPropertyValue("dc:created", created);
        doc.setPropertyValue("dc:modified", created);
        // We don't setup contributors list (no time)

        // =========================================== affaire
        doc.setPropertyValue("affaire:type_affaire", typeAffaire);
        doc.setPropertyValue("affaire:type_operation",
                TYPE_OPERATION[RandomValues.randomInt(0, TYPE_OPERATION_MAX)]);
        if (isAffairePrestExt) {
            doc.setPropertyValue("affaireprestext:participant", theUser);
        }
        // We put just one Autorité
        String[] theseAutorites = new String[1];
        theseAutorites[0] = AUTORITES[RandomValues.randomInt(0, AUTORITES_MAX)];
        doc.setPropertyValue("affaire:autorites_concernees", theseAutorites);

        doc.setPropertyValue("affaire:date_saisie", created);
        doc.setPropertyValue("affaire:detruit", false);
        // doc.setPropertyValue("affaire:detruit_quand");
        doc.setPropertyValue("affaire:echeance_de_reponse",
                RandomValues.addDays(created, 10));
        doc.setPropertyValue("affaire:lieu_de_saisie", "Lieu de saisie "
                + RandomValues.randomInt(1, 10));
        doc.setPropertyValue("affaire:nb_pieces_saisies",
                RandomValues.randomInt(20, 300));
        doc.setPropertyValue("affaire:nom_agent",
                "Agent " + RandomValues.randomInt(1, 20));
        doc.setPropertyValue("affaire:pays_provenance",
                FROM_COUNTRIES[RandomValues.randomInt(0, FROM_COUNTRIES_MAX)]);

        // Set geo coordinates origin
        {
            ArrayProperty property = (ArrayProperty) doc.getProperty("affaire:geo_provenance");
            int index = RandomValues.randomInt(0, FROM_COUNTRIES_MAX);
            List<Double> list = new ArrayList<>();
            list.add(FROM_COUNTRIES_GEO[index][0]);
            list.add(FROM_COUNTRIES_GEO[index][1]);
            property.setValue(list);
        }

        // Set geo coordinates destination
        {
            ArrayProperty property = (ArrayProperty) doc.getProperty("affaire:geo_destination");
            int index = RandomValues.randomInt(0, TO_COUNTRIES_MAX);
            List<Double> list = new ArrayList<>();
            list.add(TO_COUNTRIES_GEO[index][0]);
            list.add(TO_COUNTRIES_GEO[index][1]);
            property.setValue(list);
        }

        doc.setPropertyValue("affaire:pays_destination",
                TO_COUNTRIES[RandomValues.randomInt(0, TO_COUNTRIES_MAX)]);

        int contrevenantsCount = RandomValues.randomInt(1, 3);
        String[] theseContrevenants = new String[contrevenantsCount];
        for (int j = 0; j < contrevenantsCount; j++) {
            theseContrevenants[j] = contrevenants[RandomValues.randomInt(0,
                    contrevenants_max)];
        }
        doc.setPropertyValue("affaire:contrevenants", theseContrevenants);

        setupMarques(doc);

        if (isAffairePrestExt) {
            doc.setPropertyValue("affaireprestext:estimation",
                    RandomValues.randomInt(1000, 50000));
            doc.setPropertyValue("affaireprestext:ref_investig", "Ref-"
                    + RandomValues.randomInt(1, 30));
        }

        doc.putContextData("UpdatingData_NoEventPlease", true);
        doc = session.createDocument(doc);
        saveTheAffaire(doc);

        // Now, change the lifecycle state
        changeLifecycleState(doc, created, isAffaireDouane, isAffairePrestExt);
    }

    protected void setupMarques(DocumentModel inDoc) {

        ArrayList<String> used = new ArrayList<String>();
        String aBrand;

        // At least one main brand
        aBrand = MAIN_MARQUES[RandomValues.randomInt(0, MAIN_MARQUES_MAX)];
        used.add(aBrand);
        addMarque(inDoc, aBrand);

        // Possibly 1-2 more
        int count = RandomValues.randomInt(0, 2);
        if (count > 0) {
            for (int i = 0; i < count; ++i) {
                do {
                    aBrand = OTHER_MARQUES[RandomValues.randomInt(0,
                            OTHER_MARQUES_MAX)];
                } while (used.contains(aBrand));
                used.add(aBrand);
                addMarque(inDoc, aBrand);
            }
        }

        count = used.size();
        String[] marques = new String[count];
        for (int i = 0; i < count; ++i) {
            marques[i] = used.get(i);
        }
        inDoc.setPropertyValue("affaire:denorm_marques", marques);

    }

    protected void addMarque(DocumentModel inDoc, String inBrand) {

        Property complexMeta = inDoc.getProperty("affaire:marques");
        HashMap<String, Serializable> oneEntry = new HashMap<String, Serializable>();
        String typeAffaire = (String) inDoc.getPropertyValue("affaire:type_affaire");
        oneEntry.put("famille_produit",
                typeAffaire + "-" + RandomValues.randomInt(1, 10));
        oneEntry.put("marque", inBrand);
        oneEntry.put("modele", "Modèle-" + +RandomValues.randomInt(10, 50));
        oneEntry.put("quantite",
                QUANTITIES[RandomValues.randomInt(0, QUANTITIES_MAX)]);
        oneEntry.put("reference", "ref-" + RandomValues.randomInt(10, 200));
        complexMeta.addValue(oneEntry);
    }

    /*
     * For anything > 2 months, we consider it's closed
     */
    protected void changeLifecycleState(DocumentModel inDoc,
            Calendar inCreation, boolean isDouane, boolean isPrestExt)
            throws DocumentException, LifeCycleException {

        Tools.changeLifecycleState(session, inDoc, inCreation, isDouane,
                isPrestExt);

        if (isDouane) {
            switch (inDoc.getCurrentLifeCycleState()) {
            case "qualification":
            case "verification":
            case "close":
            case "Ouvert":
            case "EnCourstraitement":
                inDoc.setPropertyValue("dc:lastContributor",
                        ANTI_CONTREFACON_USERS[RandomValues.randomInt(0,
                                ANTI_CONTREFACON_USERS_MAX)]);
                break;

            default:
                inDoc.setPropertyValue("dc:lastContributor", theUser);
                break;
            }
        }

        saveTheAffaire(inDoc);

    }

    protected void setUpAffairesPath() {

        String nxql = "SELECT * FROM Affaires WHERE dc:title = 'Affaires'";
        DocumentModelList docs = session.query(nxql);
        if (docs.size() == 0) {
            throw new ClientException("We need an 'Affaires' document");
        } else if (docs.size() > 1) {
            throw new ClientException(
                    "We need only one 'Affaires' document with 'Afaires' title");
        }

        parentPath = docs.get(0).getPathAsString();
    }

    protected void setupContrevenants() {
        // Setup the "contrevenants". We want 50 of them, but a bit more of "1",
        // "6", and 42"
        contrevenants = new String[60];
        contrevenants_max = contrevenants.length - 1;
        for (int i = 0; i < 50; i++) {
            contrevenants[i] = "contrevenant" + (i + 1);
        }
        for (int i = 50; i < 55; i++) {
            contrevenants[i] = "contrevenant42";
        }
        for (int i = 55; i < 58; i++) {
            contrevenants[i] = "contrevenant1";
        }
        for (int i = 58; i < 60; i++) {
            contrevenants[i] = "contrevenant6";
        }

    }

    protected void saveTheAffaire(DocumentModel inDoc) {

        // Disable dublincore
        inDoc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                true);
        // Make sure our custom events are not triggered
        // (see Studio project) +> we don't want to start a workflow for example
        inDoc.putContextData("UpdatingData_NoEventPlease", true);
        // MARCHE PÔ...

        session.saveDocument(inDoc);

        if ((count % COMMIT_MODULO) == 0) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

    }

}
