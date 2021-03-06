package com.mysema.query.group;

import java.util.ArrayList;
import java.util.List;

import com.mysema.query.ResultTransformer;
import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;
import com.mysema.query.types.ExpressionBase;
import com.mysema.query.types.FactoryExpression;
import com.mysema.query.types.Operation;
import com.mysema.query.types.Ops;
import com.mysema.query.types.Visitor;

/**
 * Base class for GroupBy result transformers
 *
 * @author tiwe
 *
 * @param <K>
 * @param <T>
 */
abstract class AbstractGroupByTransformer<K, T> implements ResultTransformer<T> {

    private static final class FactoryExpressionAdapter<T> extends ExpressionBase<T> implements FactoryExpression<T> {
        private final FactoryExpression<T> expr;

        private final List<Expression<?>> args;

        private FactoryExpressionAdapter(FactoryExpression<T> expr, List<Expression<?>> args) {
            super(expr.getType());
            this.expr = expr;
            this.args = args;
        }

        @Override
        public <R, C> R accept(Visitor<R, C> v, C context) {
            return expr.accept(v, context);
        }

        @Override
        public List<Expression<?>> getArgs() {
            return args;
        }

        @Override
        public T newInstance(Object... args) {
            return expr.newInstance(args);
        }
    }

    protected final List<GroupExpression<?, ?>> groupExpressions = new ArrayList<GroupExpression<?, ?>>();

    protected final List<QPair<?,?>> maps = new ArrayList<QPair<?,?>>();

    protected final Expression<?>[] expressions;

    AbstractGroupByTransformer(Expression<K> key, Expression<?>... expressions) {
        List<Expression<?>> projection = new ArrayList<Expression<?>>(expressions.length);
        groupExpressions.add(new GOne<K>(key));
        projection.add(key);

        for (Expression<?> expr : expressions) {
            if (expr instanceof GroupExpression<?,?>) {
                GroupExpression<?,?> groupExpr = (GroupExpression<?,?>)expr;
                groupExpressions.add(groupExpr);
                Expression<?> colExpression = groupExpr.getExpression();
                if (colExpression instanceof Operation && ((Operation)colExpression).getOperator() == Ops.ALIAS) {
                    projection.add(((Operation)colExpression).getArg(0));
                } else {
                    projection.add(colExpression);
                }
                if (groupExpr instanceof GMap) {
                    maps.add((QPair<?, ?>) colExpression);
                }
            } else {
                groupExpressions.add(new GOne(expr));
                projection.add(expr);
            }
        }

        this.expressions = projection.toArray(new Expression[projection.size()]);
    }

    protected static FactoryExpression<Tuple> withoutGroupExpressions(final FactoryExpression<Tuple> expr) {
        List<Expression<?>> args = new ArrayList<Expression<?>>(expr.getArgs().size());
        for (Expression<?> arg : expr.getArgs()) {
            if (arg instanceof GroupExpression) {
                args.add(((GroupExpression)arg).getExpression());
            } else {
                args.add(arg);
            }
        }
        return new FactoryExpressionAdapter<Tuple>(expr, args);
    }

}
