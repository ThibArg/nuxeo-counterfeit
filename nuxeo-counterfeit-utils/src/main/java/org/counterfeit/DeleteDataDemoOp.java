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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 */
@Operation(id = DeleteDataDemoOp.ID, category = Constants.CAT_SERVICES, label = "DeleteDataDemoOp", description = "")
public class DeleteDataDemoOp {

    public static final String ID = "DeleteDataDemoOp";

    private static final Log log = LogFactory.getLog(DeleteDataDemoOp.class);

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run() {

        log.warn("Deleting the data demo...");

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModelList docs;
        int size = 0;
        do {
            docs = session.query(
                    "SELECT * FROM Document WHERE ecm:primaryType in ('Affaire', 'AffairePrestExt')",
                    250);
            size = docs.size();
            if (size > 0) {

                log.warn("Deleting " + size + " Affaire/AffairePrestExt...");

                DocumentRef[] docRefs = new DocumentRef[size];
                for (int i = 0; i < size; i++) {
                    docRefs[i] = docs.get(i).getRef();
                }
                session.removeDocuments(docRefs);
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

        } while (size > 0);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("Deleting the data demo -> done");

    }

}
