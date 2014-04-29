/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core.it.v4;

import org.apache.olingo.client.api.communication.header.ODataPreferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.request.retrieve.v4.ODataDeltaRequest;
import org.apache.olingo.commons.api.domain.v4.ODataDelta;
import org.apache.olingo.commons.api.domain.v4.ODataEntitySet;
import org.apache.olingo.commons.api.domain.v4.ODataProperty;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.commons.api.format.ODataPubFormat;
import org.junit.Test;

public class DeltaTestITCase extends AbstractTestITCase {

  private void parse(final ODataPubFormat format) {
    final ODataEntitySetRequest<ODataEntitySet> req = client.getRetrieveRequestFactory().getEntitySetRequest(
            client.getURIBuilder(testStaticServiceRootURL).appendEntitySetSegment("Customers").build());
    req.setPrefer(client.newPreferences().trackChanges());
    
    final ODataEntitySet customers = req.execute().getBody();
    assertNotNull(customers);
    assertNotNull(customers.getDeltaLink());

    final ODataDeltaRequest deltaReq = client.getRetrieveRequestFactory().getDeltaRequest(customers.getDeltaLink());
    deltaReq.setFormat(format);

    final ODataDelta delta = deltaReq.execute().getBody();
    assertNotNull(delta);

    assertNotNull(delta.getDeltaLink());
    assertTrue(delta.getDeltaLink().isAbsolute());
    assertEquals(5, delta.getCount(), 0);

    assertEquals(1, delta.getDeletedEntities().size());
    assertTrue(delta.getDeletedEntities().get(0).getId().isAbsolute());
    assertTrue(delta.getDeletedEntities().get(0).getId().toASCIIString().endsWith("Customers('ANTON')"));

    assertEquals(1, delta.getAddedLinks().size());
    assertTrue(delta.getAddedLinks().get(0).getSource().isAbsolute());
    assertTrue(delta.getAddedLinks().get(0).getSource().toASCIIString().endsWith("Customers('BOTTM')"));
    assertEquals("Orders", delta.getAddedLinks().get(0).getRelationship());

    assertEquals(1, delta.getDeletedLinks().size());
    assertTrue(delta.getDeletedLinks().get(0).getSource().isAbsolute());
    assertTrue(delta.getDeletedLinks().get(0).getSource().toASCIIString().endsWith("Customers('ALFKI')"));
    assertEquals("Orders", delta.getDeletedLinks().get(0).getRelationship());

    assertEquals(2, delta.getEntities().size());
    ODataProperty property = delta.getEntities().get(0).getProperty("ContactName");
    assertNotNull(property);
    assertTrue(property.hasPrimitiveValue());
    property = delta.getEntities().get(1).getProperty("ShippingAddress");
    assertNotNull(property);
    assertTrue(property.hasComplexValue());
  }

  @Test
  public void atomParse() {
    parse(ODataPubFormat.ATOM);
  }

  @Test
  public void jsonParse() {
    parse(ODataPubFormat.JSON);
  }
}