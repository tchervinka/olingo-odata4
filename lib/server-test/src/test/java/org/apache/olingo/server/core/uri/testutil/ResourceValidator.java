/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.server.core.uri.testutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmElement;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoKind;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceLambdaAll;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.UriResourceSingleton;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.core.uri.UriResourceWithKeysImpl;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.validator.UriValidationException;
import org.apache.olingo.server.core.uri.validator.UriValidator;

public class ResourceValidator implements TestValidator {
  private final OData odata = OData.newInstance();
  private Edm edm;
  private TestValidator invokedBy;
  private UriInfo uriInfo = null;

  private UriResource uriPathInfo = null;
  private int uriResourceIndex;

  // --- Setup ---

  public ResourceValidator setUpValidator(final TestValidator uriValidator) {
    invokedBy = uriValidator;
    return this;
  }

  public ResourceValidator setEdm(final Edm edm) {
    this.edm = edm;
    return this;
  }

  public ResourceValidator setUriInfoPath(final UriInfoResource uriInfoPath) {
    uriInfo = (UriInfo) uriInfoPath;
    last();
    return this;
  }

  // --- Execution ---

  public ResourceValidator run(final String path) {
    Parser testParser = new Parser(edm, odata);

    UriInfo uriInfoTmp = null;
    uriPathInfo = null;
    try {
      uriInfoTmp = testParser.parseUri(path, null, null);
    } catch (final ODataLibraryException e) {
      fail("Exception occurred while parsing the URI: " + path + "\n"
          + " Message: " + e.getMessage());
    }

    try {
      new UriValidator().validate(uriInfoTmp, HttpMethod.GET);
    } catch (final UriValidationException e) {
      fail("Exception occurred while validating the URI: " + path + "\n"
          + " Message: " + e.getMessage());
    }

    if (uriInfoTmp.getKind() != UriInfoKind.resource) {
      fail("Invalid UriInfoKind: " + uriInfoTmp.getKind().toString());
    }
    uriInfo = uriInfoTmp;

    first();
    return this;
  }

  // --- Navigation ---

  public TestUriValidator goUpUriValidator() {
    return (TestUriValidator) invokedBy;
  }

  public ExpandValidator goUpExpandValidator() {
    return (ExpandValidator) invokedBy;
  }

  public FilterValidator goUpFilterValidator() {
    return (FilterValidator) invokedBy;
  }

  public FilterValidator goParameter(final int index) {
    assertEquals(UriResourceKind.function, uriPathInfo.getKind());
    UriResourceFunction function = (UriResourceFunction) uriPathInfo;

    return new FilterValidator()
    .setEdm(edm)
    .setExpression(function.getParameters().get(index).getExpression())
    .setValidator(this);
  }

  public FilterValidator goLambdaExpression() {
    if (uriPathInfo.getKind() == UriResourceKind.lambdaAll) {
      return new FilterValidator()
      .setEdm(edm)
      .setExpression(((UriResourceLambdaAll) uriPathInfo).getExpression());

    } else if (uriPathInfo.getKind() == UriResourceKind.lambdaAny) {
      return new FilterValidator()
      .setEdm(edm)
      .setExpression(((UriResourceLambdaAny) uriPathInfo).getExpression());
    } else {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }
    return null;
  }

  public ResourceValidator goSelectItem(final int index) {
    final SelectOption select = uriInfo.getSelectOption();
    SelectItem item = select.getSelectItems().get(index);
    return new ResourceValidator()
        .setUpValidator(this)
        .setEdm(edm)
        .setUriInfoPath(item.getResourcePath());
  }

  public ExpandValidator goExpand() {
    final ExpandOption expand = uriInfo.getExpandOption();
    if (expand == null) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    return new ExpandValidator().setUpValidator(this).setExpand(expand);
  }

  public ResourceValidator first() {
    return at(0);
  }

  public ResourceValidator last() {
    final List<UriResource> parts = uriInfo.getUriResourceParts();
    if (parts.isEmpty()) {
      uriResourceIndex = 0;
      fail("not enough segments");
    } else {
      uriResourceIndex = parts.size() - 1;
      uriPathInfo = parts.get(uriResourceIndex);
    }
    return this;
  }

  public ResourceValidator n() {
    return at(uriResourceIndex + 1);
  }

  public ResourceValidator at(final int index) {
    uriResourceIndex = index;
    if (index < uriInfo.getUriResourceParts().size()) {
      uriPathInfo = uriInfo.getUriResourceParts().get(index);
    } else {
      fail("not enough segments");
    }
    return this;
  }

  // --- Validation ---

  public ResourceValidator isLambdaVar(final String var) {
    String actualVar = null;
    if (uriPathInfo.getKind() == UriResourceKind.lambdaAll) {
      actualVar = ((UriResourceLambdaAll) uriPathInfo).getLambdaVariable();
    } else if (uriPathInfo.getKind() == UriResourceKind.lambdaAny) {
      actualVar = ((UriResourceLambdaAny) uriPathInfo).getLambdaVariable();
    } else {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    assertEquals(var, actualVar);
    return this;
  }

  public ResourceValidator isTypeFilter(final FullQualifiedName expectedType) {

    if (uriPathInfo.getKind() != UriResourceKind.complexProperty &&
        uriPathInfo.getKind() != UriResourceKind.singleton) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    EdmType actualType = null;
    if (uriPathInfo instanceof UriResourceComplexProperty) {
      actualType = ((UriResourceComplexProperty) uriPathInfo).getComplexTypeFilter();
    } else if (uriPathInfo instanceof UriResourceSingleton) {
      actualType = ((UriResourceSingleton) uriPathInfo).getEntityTypeFilter();
    }

    if (actualType == null) {
      fail("type information not set");
    }

    assertEquals(expectedType, actualType.getFullQualifiedName());
    return this;
  }

  public ResourceValidator isType(final FullQualifiedName type) {
    if (!(uriPathInfo instanceof UriResourcePartTyped)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }
    UriResourcePartTyped uriPathInfoTyped = (UriResourcePartTyped) uriPathInfo;

    EdmType actualType = uriPathInfoTyped.getType();
    if (actualType == null) {
      fail("type information not set");
    }
    assertEquals(type, actualType.getFullQualifiedName());

    return this;
  }

  public ResourceValidator isType(final FullQualifiedName type, final boolean isFinallyACollection) {
    isType(type);
    assertEquals(isFinallyACollection, ((UriResourcePartTyped) uriPathInfo).isCollection());
    return this;
  }

  public ResourceValidator isTypeFilterOnEntry(final FullQualifiedName type) {
    if (!(uriPathInfo instanceof UriResourceWithKeysImpl)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceWithKeysImpl uriPathInfoKeyPred = (UriResourceWithKeysImpl) uriPathInfo;

    // input parameter type may be null in order to assert that the singleTypeFilter is not set
    EdmType actualType = uriPathInfoKeyPred.getTypeFilterOnEntry();
    assertEquals(type, type == null ? actualType : actualType.getFullQualifiedName());

    return this;
  }

  public ResourceValidator isTypeFilterOnCollection(final FullQualifiedName expectedType) {
    if (!(uriPathInfo instanceof UriResourceWithKeysImpl)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }
    UriResourceWithKeysImpl uriPathInfoKeyPred = (UriResourceWithKeysImpl) uriPathInfo;

    // input parameter type may be null in order to assert that the collectionTypeFilter is not set
    EdmType actualType = uriPathInfoKeyPred.getTypeFilterOnCollection();
    assertEquals(expectedType,
        expectedType == null || actualType == null ? actualType : actualType.getFullQualifiedName());

    return this;
  }

  // other functions
  public ResourceValidator isKeyPredicateRef(final int index, final String name, final String refencedProperty) {
    if (!(uriPathInfo instanceof UriResourceWithKeysImpl)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceWithKeysImpl info = (UriResourceWithKeysImpl) uriPathInfo;
    List<UriParameter> keyPredicates = info.getKeyPredicates();
    assertEquals(name, keyPredicates.get(index).getName());
    assertEquals(refencedProperty, keyPredicates.get(index).getReferencedProperty());
    return this;
  }

  public ResourceValidator isKeyPredicateAlias(final int index, final String name, final String alias) {
    if (!(uriPathInfo instanceof UriResourceWithKeysImpl)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceWithKeysImpl info = (UriResourceWithKeysImpl) uriPathInfo;
    List<UriParameter> keyPredicates = info.getKeyPredicates();
    assertEquals(name, keyPredicates.get(index).getName());
    assertEquals(alias, keyPredicates.get(index).getAlias());
    return this;

  }

  public ResourceValidator isKeyPredicate(final int index, final String name, final String text) {
    if (!(uriPathInfo instanceof UriResourceWithKeysImpl)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceWithKeysImpl info = (UriResourceWithKeysImpl) uriPathInfo;
    List<UriParameter> keyPredicates = info.getKeyPredicates();
    assertEquals(name, keyPredicates.get(index).getName());
    assertEquals(text, keyPredicates.get(index).getText());
    return this;

  }

  public ResourceValidator isParameter(final int index, final String name, final String text) {
    if (!(uriPathInfo instanceof UriResourceFunction)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceFunction info = (UriResourceFunction) uriPathInfo;
    List<UriParameter> keyPredicates = info.getParameters();
    assertEquals(name, keyPredicates.get(index).getName());
    assertEquals(text, keyPredicates.get(index).getText());
    return this;

  }

  public ResourceValidator isParameterAlias(final int index, final String name, final String alias) {
    if (!(uriPathInfo instanceof UriResourceFunction)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceFunction info = (UriResourceFunction) uriPathInfo;
    List<UriParameter> keyPredicates = info.getParameters();
    assertEquals(name, keyPredicates.get(index).getName());
    assertEquals(alias, keyPredicates.get(index).getAlias());
    return this;

  }

  public ResourceValidator isKind(final UriInfoKind kind) {
    assertEquals(kind, uriInfo.getKind());
    return this;
  }

  public ResourceValidator isPrimitiveProperty(final String name,
      final FullQualifiedName type, final boolean isCollection) {
    if (!(uriPathInfo instanceof UriResourcePrimitiveProperty)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourcePrimitiveProperty uriPathInfoProp = (UriResourcePrimitiveProperty) uriPathInfo;

    EdmElement property = uriPathInfoProp.getProperty();

    assertEquals(name, property.getName());
    assertEquals(type, property.getType().getFullQualifiedName());
    assertEquals(isCollection, property.isCollection());
    return this;
  }

  public ResourceValidator isComplexProperty(final String name, final FullQualifiedName type,
      final boolean isCollection) {
    if (!(uriPathInfo instanceof UriResourceComplexProperty)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceComplexProperty uriPathInfoProp = (UriResourceComplexProperty) uriPathInfo;

    EdmElement property = uriPathInfoProp.getProperty();

    assertEquals(name, property.getName());
    assertEquals(type, property.getType().getFullQualifiedName());
    assertEquals(isCollection, property.isCollection());
    return this;
  }

  public ResourceValidator isNavProperty(final String name, final FullQualifiedName type, final boolean isCollection) {
    if (!(uriPathInfo instanceof UriResourceNavigation)) {
      fail("invalid resource kind: " + uriPathInfo.getKind().toString());
    }

    UriResourceNavigation uriPathInfoProp = (UriResourceNavigation) uriPathInfo;

    EdmElement property = uriPathInfoProp.getProperty();

    assertEquals(name, property.getName());
    assertEquals(type, property.getType().getFullQualifiedName());
    assertEquals(isCollection, uriPathInfoProp.isCollection());
    return this;
  }

  public ResourceValidator isUriPathInfoKind(final UriResourceKind infoType) {
    assertNotNull(uriPathInfo);
    assertEquals(infoType, uriPathInfo.getKind());
    return this;
  }

  public ResourceValidator isAction(final String name) {
    isUriPathInfoKind(UriResourceKind.action);
    assertEquals(name, ((UriResourceAction) uriPathInfo).getAction().getName());
    return this;
  }

  public ResourceValidator isFunction(final String name) {
    isUriPathInfoKind(UriResourceKind.function);
    assertEquals(name, ((UriResourceFunction) uriPathInfo).getFunction().getName());
    return this;
  }

  public ResourceValidator isFunctionImport(final String name) {
    isUriPathInfoKind(UriResourceKind.function);
    assertEquals(name, ((UriResourceFunction) uriPathInfo).getFunctionImport().getName());
    return this;
  }

  public ResourceValidator isEntitySet(final String name) {
    isUriPathInfoKind(UriResourceKind.entitySet);
    assertEquals(name, ((UriResourceEntitySet) uriPathInfo).getEntitySet().getName());
    return this;
  }

  public ResourceValidator isComplex(final String name) {
    isUriPathInfoKind(UriResourceKind.complexProperty);
    assertEquals(name, ((UriResourceComplexProperty) uriPathInfo).getProperty().getName());
    return this;
  }

  public ResourceValidator isSingleton(final String name) {
    isUriPathInfoKind(UriResourceKind.singleton);
    assertEquals(name, ((UriResourceSingleton) uriPathInfo).getSingleton().getName());
    return this;
  }

  public ResourceValidator isValue() {
    return isUriPathInfoKind(UriResourceKind.value);
  }

  public ResourceValidator isCount() {
    return isUriPathInfoKind(UriResourceKind.count);
  }

  public ResourceValidator isRef() {
    return isUriPathInfoKind(UriResourceKind.ref);
  }

  public ResourceValidator isActionImport(final String actionName) {
    isUriPathInfoKind(UriResourceKind.action);
    assertEquals(actionName, ((UriResourceAction) uriPathInfo).getActionImport().getName());
    return this;
  }

  public ResourceValidator isIt() {
    return isUriPathInfoKind(UriResourceKind.it);
  }

  public ResourceValidator isTopText(final String topText) {
    assertEquals(topText, uriInfo.getTopOption().getText());
    return this;
  }

  public ResourceValidator isFormatText(final String formatText) {
    assertEquals(formatText, uriInfo.getFormatOption().getText());
    return this;
  }

  public ResourceValidator isInlineCountText(final String inlineCountText) {
    assertEquals(inlineCountText, uriInfo.getCountOption().getText());
    return this;
  }

  public ResourceValidator isSkipText(final String skipText) {
    assertEquals(skipText, uriInfo.getSkipOption().getText());
    return this;
  }

  public ResourceValidator isSkipTokenText(final String skipTokenText) {
    assertEquals(skipTokenText, uriInfo.getSkipTokenOption().getText());
    return this;
  }

  public ResourceValidator isSelectItemStar(final int index) {
    final SelectOption select = uriInfo.getSelectOption();
    SelectItem item = select.getSelectItems().get(index);
    assertTrue(item.isStar());
    return this;
  }

  public ResourceValidator isSelectItemAllOp(final int index, final FullQualifiedName fqn) {
    final SelectOption select = uriInfo.getSelectOption();
    SelectItem item = select.getSelectItems().get(index);
    assertEquals(fqn, item.getAllOperationsInSchemaNameSpace());
    return this;
  }

  public ResourceValidator isSelectStartType(final int index, final FullQualifiedName fullName) {
    final SelectOption select = uriInfo.getSelectOption();
    SelectItem item = select.getSelectItems().get(index);
    EdmType actualType = item.getStartTypeFilter();
    assertEquals(fullName, actualType.getFullQualifiedName());
    return this;
  }

  public ResourceValidator isInAliasToValueMap(final String alias, final String value) {
    String valueForAlias = uriInfo.getValueForAlias(alias);
    assertEquals(value, valueForAlias);
    return this;
  }
}
