package com.kulics.feel.node

import com.kulics.feel.visitor.Identifier
import com.kulics.feel.visitor.Pattern
import com.kulics.feel.visitor.Type

sealed class StatementNode : Node()

class VariableStatementNode(
    val id: Identifier,
    val initValue: ExpressionNode
) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

class ExpressionStatementNode(val expr: ExpressionNode) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

class AssignmentStatementNode(
    val id: Identifier,
    val newValue: ExpressionNode
) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

class FunctionStatementNode(
    val id: Identifier,
    val parameterTypes: List<ParameterDeclarationNode>,
    val returnType: Type,
    val body: ExpressionNode
) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

class WhileStatementNode(val cond: ExpressionNode, val stats: List<StatementNode>) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}