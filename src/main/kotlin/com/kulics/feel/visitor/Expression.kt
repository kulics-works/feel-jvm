package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*
import org.antlr.v4.runtime.tree.ParseTree

internal fun DelegateVisitor.visitExpression(ctx: ExpressionContext): ExpressionNode = when (ctx.childCount) {
    1 -> visitSingleExpression(ctx.getChild(0))
    2 -> if (ctx.callSuffix() != null) {
        visitFunctionCallExpression(ctx.expression(0), ctx.callSuffix())
    } else {
        visitMemberAccessExpression(ctx.expression(0), ctx.memberAccess())
    }
    3 -> visitBinaryExpression(ctx.expression(0), ctx.getChild(1), ctx.expression(1))
    else -> throw CompilingCheckException()
}

fun DelegateVisitor.visitSingleExpression(expr: ParseTree): ExpressionNode {
    return when (expr) {
        is PrimaryExpressionContext -> visitPrimaryExpression(expr)
        is ParenExpressionContext -> visitParenExpression(expr)
        is BlockExpressionContext -> visitBlockExpression(expr)
        is IfExpressionContext -> visitIfExpression(expr)
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitFunctionCallExpression(
    exprCtx: ExpressionContext,
    callCtx: CallSuffixContext
): ExpressionNode {
    val expr = visitExpression(exprCtx)
    val callArgs = visitCallSuffix(callCtx)
    return when (val type = expr.type) {
        is FunctionType -> {
            if (callArgs.first.isNotEmpty()) {
                println("the type of expression is not a generics function")
                throw CompilingCheckException()
            }
            if (type.parameterTypes.size != callArgs.second.size) {
                println("the size of args is ${callArgs.second.size}, but need ${type.parameterTypes.size}")
                throw CompilingCheckException()
            }
            for ((i, v) in type.parameterTypes.withIndex()) {
                if (callArgs.second[i].type.cannotAssignTo(v)) {
                    println("the type of args${i}: '${callArgs.second[i].type.name}' is not '${v.name}'")
                    throw CompilingCheckException()
                }
            }
            CallExpressionNode(expr, callArgs.second, type.returnType)
        }
        is GenericsType -> {
            if (type.typeParameter.size != callArgs.first.size) {
                println("the type args size need '${type.typeParameter.size}', but found '${callArgs.first.size}'")
                throw CompilingCheckException()
            }
            for (v in callArgs.first) {
                if (v is GenericsType) {
                    println("the generics type '${v.name}' can not be type args")
                    throw CompilingCheckException()
                }
            }
            val instanceType = type.typeConstructor(callArgs.first)
            if (instanceType !is FunctionType) {
                println("the type of expression is not a generics function")
                throw CompilingCheckException()
            }
            for ((i, v) in instanceType.parameterTypes.withIndex()) {
                if (callArgs.second[i].type.cannotAssignTo(v)) {
                    println("the type of args${i}: '${callArgs.second[i].type.name}' is not '${v.name}'")
                    throw CompilingCheckException()
                }
            }
            GenericsCallExpressionNode(expr, callArgs.first, callArgs.second, instanceType.returnType)
        }
        else -> {
            println("the type of expression is not a function")
            throw CompilingCheckException()
        }
    }
}

fun DelegateVisitor.visitMemberAccessExpression(
    exprCtx: ExpressionContext,
    memberCtx: MemberAccessContext
): ExpressionNode {
    val expr = visitExpression(exprCtx)
    val memberIdentifier = visitMemberAccess(memberCtx)
    val member = expr.type.getMember(memberIdentifier)
    if (member != null) {
        return MemberExpressionNode(expr, member)
    } else {
        println("the type '${expr.type.name}' have not member '${memberIdentifier}'")
        throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitBinaryExpression(
    lhs: ExpressionContext,
    op: ParseTree,
    rhs: ExpressionContext
): ExpressionNode {
    val lhsExpr = visitExpression(lhs)
    val rhsExpr = visitExpression(rhs)
    return when (op) {
        is AdditiveOperatorContext -> {
            checkCalculateExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.Add() != null) AdditiveOperator.Add
            else AdditiveOperator.Sub
            AdditiveExpressionNode(lhsExpr, rhsExpr, symbol, lhsExpr.type)
        }
        is MultiplicativeOperatorContext -> {
            checkCalculateExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.Mul() != null) MultiplicativeOperator.Mul
            else if (op.Div() != null) MultiplicativeOperator.Div
            else MultiplicativeOperator.Mod
            MultiplicativeExpressionNode(lhsExpr, rhsExpr, symbol, lhsExpr.type)
        }
        is CompareOperatorContext -> {
            checkCompareExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.EqualEqual() != null) CompareOperator.Equal
            else if (op.NotEqual() != null) CompareOperator.NotEqual
            else if (op.Less() != null) CompareOperator.Less
            else if (op.LessEqual() != null) CompareOperator.LessEqual
            else if (op.Greater() != null) CompareOperator.Greater
            else CompareOperator.GreaterEqual
            CompareExpressionNode(lhsExpr, rhsExpr, symbol)
        }
        is LogicOperatorContext -> {
            checkLogicExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.AndAnd() != null) LogicOperator.And
            else LogicOperator.Or
            LogicExpressionNode(lhsExpr, rhsExpr, symbol)
        }
        else -> throw CompilingCheckException()
    }
}

fun checkCalculateExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeInt -> if (rhs.type != builtinTypeInt) {
            println("the type of right value is not '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }
        builtinTypeFloat -> if (rhs.type != builtinTypeFloat) {
            println("the type of right value is not '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeInt.name}' or '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
    }
}

fun checkCompareExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeInt -> if (rhs.type != builtinTypeInt) {
            println("the type of right value is not '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }
        builtinTypeFloat -> if (rhs.type != builtinTypeFloat) {
            println("the type of right value is not '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeInt.name}' or '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
    }
}

fun checkLogicExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeBool -> if (rhs.type != builtinTypeBool) {
            println("the type of right value is not '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
    }
}

internal fun DelegateVisitor.visitCallSuffix(ctx: CallSuffixContext): Pair<List<Type>, List<ExpressionNode>> {
    return (ctx.type().map {
        checkType(visitType(it))
    }) to (ctx.expression().map { visitExpression(it) })
}

internal fun DelegateVisitor.visitMemberAccess(ctx: MemberAccessContext): String {
    return visitIdentifier(ctx.identifier())
}

internal fun DelegateVisitor.visitParenExpression(ctx: ParenExpressionContext): ExpressionNode {
    return ParenExpressionNode(visitExpression(ctx.expression()))
}

internal fun DelegateVisitor.visitPrimaryExpression(ctx: PrimaryExpressionContext): ExpressionNode {
    return if (ctx.literalExpression() != null) {
        visitLiteralExpression(ctx.literalExpression())
    } else {
        val name = visitIdentifier(ctx.identifier())
        val id = getIdentifier(name)
        if (id == null) {
            println("the identifier '${name}' is not define")
            throw CompilingCheckException()
        } else {
            IdentifierExpressionNode(id)
        }
    }
}

internal fun DelegateVisitor.visitLiteralExpression(ctx: LiteralExpressionContext): ExpressionNode {
    return if (ctx.integerExpression() != null) {
        LiteralExpressionNode(ctx.integerExpression().text, builtinTypeInt)
    } else if (ctx.floatExpression() != null) {
        LiteralExpressionNode(ctx.floatExpression().text, builtinTypeFloat)
    } else if (ctx.characterExpression() != null) {
        LiteralExpressionNode(ctx.characterExpression().text, builtinTypeChar)
    } else {
        LiteralExpressionNode(ctx.stringExpression().text, builtinTypeString)
    }
}

internal fun DelegateVisitor.visitIfExpression(ctx: IfExpressionContext): ExpressionNode {
    val cond = visitExpression(ctx.expression(0))
    return if (ctx.pattern() == null) {
        if (cond.type != builtinTypeBool) {
            println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
        val thenBranch = visitExpression(ctx.expression(1))
        val elseBranch = visitExpression(ctx.expression(2))
        if (thenBranch.type != elseBranch.type) {
            println("the type of then branch is '${thenBranch.type.name}', and the type of else branch is '${elseBranch.type.name}', they are not equal")
            throw CompilingCheckException()
        }
        ConditionExpressionNode(cond, thenBranch, elseBranch, thenBranch.type)
    } else {
        pushScope()
        val pattern = visitPattern(ctx.pattern())
        if (pattern is IdentifierPattern) {
            val identifier = Identifier(pattern.identifier, cond.type)
            addIdentifier(identifier)
        }
        val thenBranch = visitExpression(ctx.expression(1))
        popScope()
        val elseBranch = visitExpression(ctx.expression(2))
        if (thenBranch.type != elseBranch.type) {
            println("the type of then branch is '${thenBranch.type.name}', and the type of else branch is '${elseBranch.type.name}', they are not equal")
            throw CompilingCheckException()
        }
        return when (pattern) {
            is TypePattern -> {
                val matchCode =
                    "val ${pattern.identifier.name} = BuiltinTool.cast<${
                        pattern.type.generateTypeName()
                    }>(${cond.generateCode()});$Wrap"
                val condExpr =
                    ConditionExpressionNode(
                        LiteralExpressionNode("${pattern.identifier.name} != null", builtinTypeBool),
                        thenBranch,
                        elseBranch,
                        thenBranch.type
                    )
                BlockExpressionNode(matchCode, condExpr)
            }
            is IdentifierPattern -> {
                val matchCode = "val ${pattern.identifier} = ${cond.generateCode()};$Wrap"
                BlockExpressionNode(matchCode, thenBranch)
            }
            is LiteralPattern -> {
                checkCompareExpressionType(cond, pattern.expr)
                ConditionExpressionNode(
                    LiteralExpressionNode("${cond.generateCode()} == ${pattern.expr.generateCode()}", builtinTypeBool),
                    thenBranch,
                    elseBranch,
                    thenBranch.type
                )
            }
            is WildcardPattern -> {
                BlockExpressionNode("${cond.generateCode()};$Wrap", thenBranch)
            }
        }
    }
}

internal fun DelegateVisitor.visitIdentifierPattern(ctx: IdentifierPatternContext): String {
    return visitIdentifier(ctx.identifier())
}

internal fun DelegateVisitor.visitBlockExpression(ctx: BlockExpressionContext): BlockExpressionNode {
    pushScope()
    val code = ctx.statement().fold(StringBuilder()) { acc, v -> acc.append("${visitStatement(v)};") }.toString()
    val node = BlockExpressionNode(
        code, when (val expr = ctx.expression()) {
            null -> null
            else -> visitExpression(expr)
        }
    )
    popScope()
    return node
}