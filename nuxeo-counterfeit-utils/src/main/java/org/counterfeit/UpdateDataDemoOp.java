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

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * We just update the dates
 * 
 */
@Operation(id=UpdateDataDemoOp.ID, category=Constants.CAT_SERVICES, label="UpdateDataDemoOp", description="")
public class UpdateDataDemoOp {

    public static final String ID = "UpdateDataDemoOp";

    private static final Log log = LogFactory.getLog(UpdateDataDemoOp.class);

    protected static final int COMMIT_MODULO = 50;

    protected long todayAsMS;

    protected int count = 0;

    protected DateFormat _yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    protected Calendar _today = Calendar.getInstance();

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run() throws DocumentException, LifeCycleException {

        log.warn("Updating ...");

        todayAsMS = Calendar.getInstance().getTimeInMillis();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String nxql = "SELECT * FROM Affaire";
        DocumentModelList docs = session.query(nxql);
        // This is not ok for thousands of documents
        count = 0;
        for (DocumentModel doc : docs) {

            Calendar created = RandomValues.buildDate(null, 0, 360, true);
            doc.setPropertyValue("dc:created", created);
            doc.setPropertyValue("dc:modified", created);
            doc.setPropertyValue("affaire:date_saisie", created);
            doc.setPropertyValue("affaire:echeance_de_reponse",
                    RandomValues.addDays(created, 10));
            
            // Disable dublincore
            doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                    true);
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);

            session.saveDocument(doc);
            
            String docType = doc.getType();
            Tools.changeLifecycleState(session, doc, created, docType.equals("Affaire"), docType.equals("AffairePrestExt"));

            count += 1;
            if ((count % 50) == 0) {
                log.warn("Updated: " + count);
            }
            if ((count % COMMIT_MODULO) == 0) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }

        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        log.warn("...updating done.");
       
    } 

}
