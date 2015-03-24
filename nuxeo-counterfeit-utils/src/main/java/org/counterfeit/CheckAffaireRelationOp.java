/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     thibaud
 */

package org.counterfeit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Renseigne les deux variables de contexte hasAutresAffaires (boolean) et
 * autresAffaires (DocumentModelList)
 */
@Operation(id = CheckAffaireRelationOp.ID, category = Constants.CAT_DOCUMENT, label = "Affaire : Vérifier relations", description = "")
public class CheckAffaireRelationOp {

    public static final String ID = "CheckAffaireRelationOp";
    
    private static final Log log = LogFactory.getLog(CheckAffaireRelationOp.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) {
        
        String type = inDoc.getType();
        if (!type.equals("Affaire") && !type.equals("AffairePrestExt")) {
            ctx.put("hasAutresAffaires", false);
            log.warn("Bieng called for an unsupported document type: " + type);
            return inDoc;
        }

        String marques = null;
        // Get all the brands. affaire:marques is a complex multivalued
        // We get an array of Complex values, which means an array of
        // Map<String, Serializable> where the String is the name of the field
        ArrayList<Map<String, Serializable>> values = (ArrayList<Map<String, Serializable>>) inDoc.getPropertyValue("affaire:marques");
        if(values != null && values.size() > 0) {
            marques = "(";
            int max = values.size();
            for(int i = 0; i < max; ++i) {
                Map<String, Serializable> oneEntry = values.get(i);
                marques += "'" + oneEntry.get("marque") + "'";
                if(i < (max -1)) {
                    marques += ",";
                }
            }
            marques += ")";
        }
        
        String contrevenantsStr = null;
        String[] contrevenants = (String[]) inDoc.getPropertyValue("affaire:contrevenants");
        if(contrevenants != null && contrevenants.length > 0) {
            contrevenantsStr = "(";
            int max = contrevenants.length;
            for(int i = 0; i < max; ++i) {
                contrevenantsStr += "'" + contrevenants[i] + "'";
                if(i < (max -1)) {
                    contrevenantsStr += ",";
                }
            }
            contrevenantsStr += ")";
        }
        
        if(false) {
            String nxql = "SELECT * FROM Affaire WHERE";
            nxql += " affaire:marque = '" + inDoc.getPropertyValue("affaire:marque") + "'";
            nxql += " AND affaire:pays_provenance = '" + inDoc.getPropertyValue("affaire:pays_provenance") + "'";
            nxql += " AND affaire:pays_destination = '" + inDoc.getPropertyValue("affaire:pays_destination") + "'";
        }

        if(marques != null || contrevenantsStr != null) {
            // Not perfect algorithm, because we actually don't link the brand and the quantity
            // OK for POC
            String nxql = "SELECT * FROM Document WHERE";
            if(marques != null) {
                nxql += " affaire:marques/*/marque IN " + marques;
            }
            if(contrevenantsStr != null) {
                nxql += " AND affaire:contrevenants IN " + contrevenantsStr;
            }
    
            DocumentModelList docs = session.query(nxql);
            
            // Remove our own ID
            String inDocId = inDoc.getId();
            int idx = -1;
            boolean found = false;
            for(DocumentModel oneDoc : docs) {
                idx += 1;
                if(oneDoc.getId().equals(inDocId)) {
                    found = true;
                    break;
                }
            }
            if(found) {
                docs.remove(idx);
            }
    
            ctx.put("hasAutresAffaires", docs.size() > 0);
            ctx.put("autresAffaires", docs);
        } else {
            ctx.put("hasAutresAffaires", false);
        }

        return inDoc;
    }

}
