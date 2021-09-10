parser grammar FeelParser;

options { tokenVocab=FeelLexer; }

program: moduleDeclaration globalDeclaration*;

moduleDeclaration: Module identifier Semi;

globalDeclaration
    : (globalVariableDeclaration
    | globalConstantDeclaration
    | globalFunctionDeclaration
    ) Semi
    ;

globalVariableDeclaration: Let Mut identifier type? Equal expression;
globalConstantDeclaration: Let identifier type? Equal expression;
globalFunctionDeclaration: Let identifier parameterList type? Equal expression;

parameterList: LeftParen (parameter (Comma parameter)*)? RightParen;

parameter: identifier type;

block: LeftBrace (statement Semi)* RightBrace;

statement
    : variableDeclaration
    | constantDeclaration
    | assignment
    | ifStatement
    | whileStatement
    | expression
    ;

expression
    : primaryExpression
    | parenExpression
    | blockExpression
    | ifExpression
    | expression callSuffix
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    | expression compareOperator expression
    | expression logicOperator expression
    ;

variableDeclaration: Let Mut identifier type? Equal expression;
constantDeclaration: Let identifier type? Equal expression;

callSuffix: LeftParen (expression (Comma expression)*)? RightParen;

assignment: identifier Equal expression;

ifStatement
    : If LeftParen expression RightParen block (Else (block | ifStatement))?
    ;

ifExpression
    : If LeftParen expression RightParen expression Else expression
    ;

whileStatement
    : While LeftParen expression RightParen block
    ;

blockExpression: LeftBrace (statement Semi)* expression? RightBrace;

primaryExpression
    : literalExpression
    | identifier
    ;

parenExpression: LeftParen expression RightParen;

literalExpression
    : integerExpression
    | floatExpression
    ;

type
    : identifier
    ;

identifier: Identifier;

floatExpression: FloatLiteral;

integerExpression: DecimalLiteral | BinaryLiteral | OctalLiteral | HexLiteral;

multiplicativeOperator: Mul | Div | Mod;

additiveOperator: Add | Sub ;

logicOperator: And | Or ;

compareOperator: Less | Greater | LessEqual | GreaterEqual | EqualEqual | NotEqual;