# OMG IDL 4.2 Conformance Matrix

| IDL construct | Clause / section | Requirement IDs | Status | Test IDs | Notes |
|---|---|---|---|---|---|
| lexical conventions | IDL-42-LEXICAL | REQ-IDL-001, REQ-IDL-003, REQ-SEC-003 | partial | `IdlLexerTest` | G6-110 covers bounded lexer tokenization and lexical diagnostics. Grammar and semantic validation remain future tasks. |
| preprocessing | IDL-42-PREPROCESSING, IDL-42-LEXICAL | REQ-IDL-001, REQ-IDL-003, REQ-SEC-003 | partial | `IdlPreprocessorTest` | G6-120 covers line continuations, safe include expansion, simple macros, selected conditionals, source mapping, and stable diagnostics. Full ISO C++ 2003 preprocessing compatibility remains future hardening. |
| modules | IDL-42-GRAMMAR, Building Block Core Data Types | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 covers syntax-only AST construction for module declarations. Semantic scoping remains future work. |
| constants | IDL-42-GRAMMAR, Building Block Core Data Types | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 records unevaluated constant-expression lexemes. Semantic expression evaluation remains future work. |
| interfaces | IDL-42-GRAMMAR, Building Block Interfaces - Basic and Full | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 covers full interface bodies with operation and attribute members. Forward declarations and inheritance remain deferred. |
| operations | IDL-42-GRAMMAR, Building Block Interfaces - Basic and Full | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 covers simple operations, parameter directions, optional `oneway`, and `raises(...)`. Context clauses remain deferred. |
| attributes | IDL-42-GRAMMAR, Building Block Interfaces - Basic and Full | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 covers readonly and read/write attributes with simple declarators. Array declarators remain deferred. |
| exceptions | IDL-42-GRAMMAR, Building Block Core Data Types | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 covers syntax-only exception declarations with simple fields. Semantic member validation remains future work. |
| structs | IDL-42-GRAMMAR, Building Block Core Data Types | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 covers syntax-only struct declarations with simple fields. Semantic member validation remains future work. |
| unions | IDL-42-GRAMMAR, Building Block Core Data Types | REQ-IDL-001 | not-started | unassigned | G3 assigns tests. |
| enums | IDL-42-GRAMMAR, Building Block Core Data Types | REQ-IDL-001 | partial | `IdlParserTest` | G6-130 covers syntax-only enum declarations and enumerator ordering. Semantic name validation remains future work. |
| sequences and arrays | IDL-42-GRAMMAR, Building Block Core Data Types | REQ-IDL-001 | not-started | unassigned | G3 assigns tests. |
| valuetypes | IDL-42-GRAMMAR, Building Block Value Types and CORBA-Specific Value Types | REQ-IDL-001 | not-started | unassigned | G3 assigns tests. |
