package jol.lang.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xtc.tree.Node;

import jol.types.basic.Tuple;
import jol.types.basic.ValueList;
import jol.types.exception.JolRuntimeException;
import jol.types.exception.PlannerException;
import jol.types.function.TupleFunction;
import jol.types.basic.Schema;

public class VList extends Expression<ValueList> {
	private List<Expression> values;
	
	public VList(Node node, List<Expression> values) {
		super(node);
		this.values = values;
	}
	
	public Expression clone() {
		List<Expression> values = new ArrayList<Expression>();
		for (Expression value : this.values) {
			values.add(value.clone());
		}
		return new VList(node(), values);
	}
	
	@Override
	public Class type() {
		return jol.types.basic.ValueList.class;
	}
	
	@Override
	public String toString() {
		return this.values.toString();
	}

	@Override
	public Set<Variable> variables() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Expression value : this.values) {
			variables.addAll(value.variables());
		}
		return variables;
	}

	@Override
	public TupleFunction function(Schema schema) throws PlannerException {
		final List<TupleFunction<Comparable>> functions = 
			new ArrayList<TupleFunction<Comparable>>();
		for (Expression value : values) {
			functions.add(value.function(schema));
		}
		
		return new TupleFunction() {
			public Object evaluate(Tuple tuple) throws JolRuntimeException {
				ValueList list = new ValueList();
				for (TupleFunction<Comparable> fn : functions) {
					list.add(fn.evaluate(tuple));
				}
				return list;
			}
			public Class returnType() {
				return ValueList.class;
			}
		};
	}

}
