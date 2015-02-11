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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 */
@Operation(id = CreateDemoDataOp.ID, category = Constants.CAT_SERVICES, label = "CreateDemoDataOp", description = "")
public class CreateDemoDataOp {

    public static final String ID = "CreateDemoDataOp";

    private static final Log log = LogFactory.getLog(CreateDemoDataOp.class);

    protected static final int COMMIT_MODULO = 50;

    protected static final String[] ANTI_CONTREFACON_USERS = { "utilisateur1",
            "utilisateur1", "utilisateur1", "utilisateur1", "utilisateur2" };

    protected static final int ANTI_CONTREFACON_USERS_MAX = ANTI_CONTREFACON_USERS.length - 1;

    protected static final String[] AUTORITES = { "Douane", "Douane", "Douane",
            "Douane", "Police", "Police", "Ministere", "Ministere", "Autre" };

    protected static final int AUTORITES_MAX = AUTORITES.length - 1;

    protected static final String[] MARQUES = { "Chaumet", "Guerlain",
            "Dom Pérignon", "Fred", "Kenzo", "Givenchy", "Henessy",
            "Louis Vuitton", "Louis Vuitton", "Louis Vuitton" };

    protected static final int MARQUES_MAX = MARQUES.length - 1;

    protected static final String[] FROM_COUNTRIES = { "africa/Botswana",
            "africa/Chad", "asia/China", "asia/China", "asia/China",
            "europe/Albania", "asia/Cambodia" };

    protected static final int FROM_COUNTRIES_MAX = FROM_COUNTRIES.length - 1;

    protected static final String[] TO_COUNTRIES = { "europe/Germany",
            "europe/Spain", "europe/Switzerland", "europe/France",
            "america/United_States_of_America", "asia/China" };

    protected static final int TO_COUNTRIES_MAX = TO_COUNTRIES.length - 1;

    protected long todayAsMS;

    protected int count = 0;

    protected String parentPath;

    protected DateFormat _yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected Calendar _today = Calendar.getInstance();

    @Context
    protected CoreSession session;

    @Param(name = "howMany", required = false, values = { "2000" })
    protected long howMany;

    @OperationMethod
    public void run() {

        log.warn("Creating " + howMany + " Affaires...");

        todayAsMS = Calendar.getInstance().getTimeInMillis();

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

    }

    protected void createOneAffaire() {

        DocumentModel doc;

        // Build the title as in Studio config
        UIDSequencer svc = Framework.getService(UIDSequencer.class);
        int next = svc.getNext("AFFAIRE");
        String title = "AFF-" + String.format("%06d", next);

        // Create the model
        doc = session.createDocumentModel(parentPath, title, "Affaire");

        // Setup values
        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("affaire:type_affaire", "Douane");

        // =========================================== dublincore
        // Always created by "douanier"
        doc.setPropertyValue("dc:creator", "douanier");
        doc.setPropertyValue("dc:lastContributor", "douanier");
        Calendar created = RandomValues.buildDate(null, 0, 360, true);
        doc.setPropertyValue("dc:created", created);
        doc.setPropertyValue("dc:modified", created);
        // We don't setup contributors list (no time)

        // =========================================== affaire
        doc.setPropertyValue("affaire:autorite_concernee",
                AUTORITES[RandomValues.randomInt(0, AUTORITES_MAX)]);
        doc.setPropertyValue("affaire:date_saisie", created);
        doc.setPropertyValue("affaire:detruit", false);
        // doc.setPropertyValue("affaire:detruit_quand");
        doc.setPropertyValue("affaire:echeance_de_reponse",
                RandomValues.addDays(created, 10));
        doc.setPropertyValue("affaire:lieu_de_saisie", "Lieu de saisie "
                + RandomValues.randomInt(1, 10));
        doc.setPropertyValue("affaire:marque",
                MARQUES[RandomValues.randomInt(0, MARQUES_MAX)]);
        doc.setPropertyValue("affaire:nb_pieces_saisies",
                RandomValues.randomInt(20, 300));
        doc.setPropertyValue("affaire:nom_agent",
                "Agent " + RandomValues.randomInt(1, 20));
        doc.setPropertyValue("affaire:pays_destination",
                FROM_COUNTRIES[RandomValues.randomInt(0, FROM_COUNTRIES_MAX)]);
        doc.setPropertyValue("affaire:pays_provenance",
                TO_COUNTRIES[RandomValues.randomInt(0, TO_COUNTRIES_MAX)]);
        int contrevenantsCount = RandomValues.randomInt(1, 3);
        String[] contrevenants = new String[contrevenantsCount];
        for (int j = 1; j < (contrevenantsCount - 1); j++) {
            contrevenants[j] = "Contrevenant - "
                    + RandomValues.randomInt(1, 30);
        }
        doc.setPropertyValue("affaire:contrevenants", contrevenants);

        doc = session.createDocument(doc);
        saveTheAffaire(doc);

        // Now, change the lifecycle state
        changeLifecycleState(doc, created);
    }

    /*
     * For anything > 2 months, we consider it's closed
     */
    protected void changeLifecycleState(DocumentModel inDoc, Calendar inCreation) {

        long diffInDays = TimeUnit.DAYS.convert(
                todayAsMS - inCreation.getTimeInMillis(), TimeUnit.MILLISECONDS);

        if (diffInDays > 60) {
            inDoc.followTransition("to_qualification");
            inDoc.followTransition("to_verification");
            inDoc.followTransition("to_intervention");
            inDoc.followTransition("to_close");
        } else {

            int r = RandomValues.randomInt(1, 100);

            // 5% "nouvelle"
            if (r > 5) {
                // 15% "qualification"
                inDoc.followTransition("to_qualification");
                if (r >= 20) {
                    // 30% "Verification,"
                    inDoc.followTransition("to_verification");
                    if (r >= 50) {
                        // 10% "intervention"
                        inDoc.followTransition("to_intervention");
                        if (r >= 60) {
                            // 40% "close"
                            inDoc.followTransition("to_close");
                        }
                    }
                }
            }
        }

        switch (inDoc.getCurrentLifeCycleState()) {
        case "qualification":
        case "verification":
        case "close":
            inDoc.setPropertyValue("dc:lastContributor",
                    ANTI_CONTREFACON_USERS[RandomValues.randomInt(0,
                            ANTI_CONTREFACON_USERS_MAX)]);
            break;

        default:
            inDoc.setPropertyValue("dc:lastContributor", "douanier");
            break;
        }
        saveTheAffaire(inDoc);

    }

    protected void updateModificator(DocumentModel inDoc, String inUser) {
        // Unused
    }

    protected void setUpAffairesPath() {

        String nxql = "SELECT * FROM Affaires";
        DocumentModelList docs = session.query(nxql);
        if (docs.size() == 0) {
            throw new ClientException("We need an 'Affaires' document");
        }

        parentPath = docs.get(0).getPathAsString();
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
