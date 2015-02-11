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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Renseigne les deux variables de contexte hasAutresAffaires (boolean) et autresAffaires (DocumentModelList)
 */
@Operation(id=CheckAffaireRelationOp.ID, category=Constants.CAT_DOCUMENT, label="Affaire : Vérifier relations", description="")
public class CheckAffaireRelationOp {

    public static final String ID = "CheckAffaireRelationOp";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) {
      
        if(!inDoc.getType().equals("Affaire")) {
            return inDoc;
        }
        
        String nxql = "SELECT * FROM Affaire WHERE";
        nxql += " affaire:marque = '" + inDoc.getPropertyValue("affaire:marque") + "'";
        nxql += " AND affaire:pays_provenance = '" + inDoc.getPropertyValue("affaire:pays_provenance") + "'";
        nxql += " AND affaire:pays_destination = '" + inDoc.getPropertyValue("affaire:pays_destination") + "'";
        
        DocumentModelList docs = session.query(nxql);

        ctx.put("hasAutresAffaires", docs.size() > 0);
        ctx.put("autresAffaires", docs);

        return inDoc;
    }    

}
