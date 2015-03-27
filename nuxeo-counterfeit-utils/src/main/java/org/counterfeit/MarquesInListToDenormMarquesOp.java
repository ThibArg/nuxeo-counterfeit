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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * This one is a workaround to a problem with Studio (and/or nuxeo). The list of
 * "marques" (brands) is stored in a complex-multivalued field, affaire:marques.
 * Unfortunately, in Studio, you can't use an elasticsearch "terms" widget using
 * affaire:marques/ * /marque for example." (notice: using "/ * /" because we're
 * inside a java comment, but there are no spaces of course). The workaround was
 * the following:
 * <ul>
 * <li>Add a affaire:dernorm_marques text field, multivalued</li>
 * <li>For "about to create" and "before modification", transfert all the
 * affaire:marques/n/marque to this field</li>
 * </ul>
 * It was easier to have an operation that doing it in Studio.
 */
@Operation(id = MarquesInListToDenormMarquesOp.ID, category = Constants.CAT_DOCUMENT, label = "MarquesInListToDenormMarquesOp", description = "Fills the affaire:denorm_marques String list field (used for es aggregation). Document is not saved.")
public class MarquesInListToDenormMarquesOp {

    public static final String ID = "MarquesInListToDenormMarquesOp";

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) {

        ArrayList<Map<String, Serializable>> values = (ArrayList<Map<String, Serializable>>) inDoc.getPropertyValue("affaire:marques");

        if (values != null && values.size() > 0) {
            int max = values.size();
            String[] brands = new String[max];
            for (int i = 0; i < max; ++i) {
                Map<String, Serializable> oneEntry = values.get(i);
                brands[i] = (String) oneEntry.get("marque");
            }
            inDoc.setPropertyValue("affaire:denorm_marques", brands);
        }

        return inDoc;
    }

}
