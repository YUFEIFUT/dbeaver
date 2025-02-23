/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.context.SourceResolutionResult;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * Describes database entity name
 */
public class SQLQueryQualifiedName extends SQLQueryLexicalScopeItem {
    @Nullable
    public final SQLQuerySymbolEntry catalogName;
    @Nullable
    public final SQLQuerySymbolEntry schemaName;
    @NotNull
    public final SQLQuerySymbolEntry entityName;

    public SQLQueryQualifiedName(@NotNull STMTreeNode syntaxNode, @NotNull SQLQuerySymbolEntry entityName) {
        this(syntaxNode, null, null, entityName);
    }

    public SQLQueryQualifiedName(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry schemaName,
        @NotNull SQLQuerySymbolEntry entityName
    ) {
        this(syntaxNode, null, schemaName, entityName);
    }

    public SQLQueryQualifiedName(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry catalogName,
        @Nullable SQLQuerySymbolEntry schemaName,
        @NotNull SQLQuerySymbolEntry entityName
    ) {
        super(syntaxNode);
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.entityName = entityName;
    }

    @NotNull
    @Override
    public STMTreeNode[] getSyntaxComponents() {
        if (catalogName != null && schemaName != null) {
            return new STMTreeNode[] {
                this.catalogName.getSyntaxNode(),
                this.schemaName.getSyntaxNode(), 
                this.entityName.getSyntaxNode() 
            };
        } else if (schemaName != null) {
            return new STMTreeNode[] {
                this.schemaName.getSyntaxNode(), 
                this.entityName.getSyntaxNode() 
            };
        } else {
            return new STMTreeNode[] { 
                this.entityName.getSyntaxNode() 
            };
        }
    }

    /**
     * Set the class to the qaulified name components
     */
    public void setSymbolClass(@NotNull SQLQuerySymbolClass symbolClass) {
        if (this.entityName != null) {
            this.entityName.getSymbol().setSymbolClass(symbolClass);
        }
        if (this.schemaName != null) {
            this.schemaName.getSymbol().setSymbolClass(symbolClass);
        }
        if (this.catalogName != null) {
            this.catalogName.getSymbol().setSymbolClass(symbolClass);
        }
    }

    /**
     * Set the definition to the qaulified name components based on the database metadata
     */
    public void setDefinition(@NotNull DBSObject realTable) {
        if (this.entityName != null) {
            this.entityName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(realTable, SQLQuerySymbolClass.TABLE));
            if (this.schemaName != null) {
                DBSObject schema = realTable.getParentObject();
                if (schema != null) {
                    this.schemaName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(schema, SQLQuerySymbolClass.SCHEMA));
                } else {
                    this.schemaName.getSymbol().setSymbolClass(SQLQuerySymbolClass.SCHEMA);
                }
                if (this.catalogName != null && schema != null) {
                    DBSObject catalog = schema.getParentObject();
                    if (catalog != null) {
                        this.catalogName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(catalog, SQLQuerySymbolClass.CATALOG));
                    } else {
                        this.catalogName.getSymbol().setSymbolClass(SQLQuerySymbolClass.CATALOG);
                    }
                }
            }
        }
    }

    /**
     * Set the definition to the qaulified name components based on the query structure
     */
    public void setDefinition(@NotNull SourceResolutionResult rr) {
        if (rr.aliasOrNull != null) {
            this.entityName.merge(rr.aliasOrNull);
        } else if (rr.source instanceof SQLQueryRowsTableDataModel tableModel) {
            if (this.entityName != null) {
                SQLQueryQualifiedName tableName = tableModel.getName();
                this.entityName.setDefinition(tableName.entityName);
                if (this.schemaName != null) {
                    SQLQuerySymbolEntry schemaDef = tableName.schemaName != null ? tableName.schemaName : tableName.entityName;
                    this.schemaName.setDefinition(schemaDef);
                    
                    if (this.catalogName != null) {
                        SQLQuerySymbolEntry catalogDef = tableName.catalogName != null ? tableName.catalogName : schemaDef;
                        this.catalogName.setDefinition(catalogDef);
                    }
                }
            }
        }
    }

    /**
     * Get list of the qualified name parts in the hierarchical order
     */
    @NotNull
    public List<String> toListOfStrings() {
        if (catalogName != null && schemaName != null) {
            return List.of(catalogName.getName(), schemaName.getName(), entityName.getName());
        } else if (schemaName != null) {
            return List.of(schemaName.getName(), entityName.getName());
        } else {
            return List.of(entityName.getName());
        }
    }

    /**
     * Get the qualified name string representation
     */
    @NotNull
    public String toIdentifierString() {
        if (catalogName != null && schemaName != null) {
            return String.join(".", catalogName.getRawName(), schemaName.getRawName(), entityName.getRawName());
        } else if (schemaName != null) {
            return String.join(".", schemaName.getRawName(), entityName.getRawName());
        } else {
            return entityName.getRawName();
        }
    }

    @Override
    public String toString() {
        return String.join(".", this.toListOfStrings());
    }

    @Override
    public int hashCode() {
        return this.toListOfStrings().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SQLQueryQualifiedName other && this.toListOfStrings().equals(other.toListOfStrings());
    }

    public boolean isNotClassified() {
        return this.entityName.isNotClassified()
            && (this.schemaName == null || this.schemaName.isNotClassified())
            && (this.catalogName == null || this.catalogName.isNotClassified());
    }
}
