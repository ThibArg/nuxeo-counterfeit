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
 *     thibaud
 */
package org.counterfeit;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;

/**
 * 
 *
 * @since TODO
 */
public class Tools {

    /*
     * WARNING: This code bypasses the sanity check done by misc. low-level
     * services in nuxeo, so you could find yourself setting a state that dopes
     * not exist.
     * 
     * This method makes _a_lot_ of assumption: The session is a LocalSession,
     * data is stored in a SQL database, etc.
     * 
     * In the context of this very specific plug-in, it is ok (notice we don't
     * say "it _probably" is ok"n or "it _should_ be ok" :->.
     */
    public static void customSetCurrentLifecycleState(CoreSession inSession,
            DocumentModel inDoc, String inState) throws DocumentException,
            LifeCycleException {

        LocalSession localSession = (LocalSession) inSession;
        Session baseSession = localSession.getSession();

        Document baseDoc = baseSession.getDocumentByUUID(inDoc.getId());
        // SQLDocument sqlDoc = (SQLDocument) baseDoc;
        // sqlDoc.setCurrentLifeCycleState(inState);
        baseDoc.setCurrentLifeCycleState(inState);

    }

    /*
     * For anything > 2 months, we consider it's closed
     */
    public static void changeLifecycleState(CoreSession inSession,
            DocumentModel inDoc, Calendar inCreation) throws DocumentException, LifeCycleException {

        long todayAsMS = Calendar.getInstance().getTimeInMillis();
        long diffInDays = TimeUnit.DAYS.convert(
                todayAsMS - inCreation.getTimeInMillis(), TimeUnit.MILLISECONDS);
        
        String theState = null;
        if (diffInDays > 60) {
            theState = "close";
        } else {
            int r = RandomValues.randomInt(1, 100);
            
         // 5% "nouvelle"
            if (r > 5) {
                // 15% "qualification"
                theState = "qualification";
                if (r >= 20) {
                    // 30% "Verification,"
                    theState = "verification";
                    if (r >= 50) {
                        // 10% "intervention"
                        theState = "intervention";
                        if (r >= 60) {
                            // 40% "close"
                            theState = "close";
                        }
                    }
                }
            } else {
                theState = "nouvelle";
            }
        }

        customSetCurrentLifecycleState(inSession, inDoc, theState);
        
        
    }

}
