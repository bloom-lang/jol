package jol.lang.plan;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import xtc.tree.Node;

import jol.types.basic.Tuple;
import jol.types.exception.JolRuntimeException;
import jol.types.function.TupleFunction;
import jol.types.basic.Schema;

public class StaticReference extends Reference {
	
	private Field field;
	
	public StaticReference(Node node, Class type, Field field) {
		super(node, field.getType(),
			  type.getCanonicalName() + "." + field.getName());
		this.field = field;
	}
	
	public Expression clone() {
		return new StaticReference(node(), type, field);
	}
	
	public Field field() {
		return this.field;
	}

	@Override
	public TupleFunction function(Schema schema) {
		return new TupleFunction() {
			public Object evaluate(Tuple tuple) throws JolRuntimeException {
				try {
					return StaticReference.this.field.get(null);
				} catch (Exception e) {
					throw new JolRuntimeException(e.toString());
				}
			}
			public Class returnType() {
				return StaticReference.this.field.getType();
			}
		};
	}

	@Override
	public Set<Variable> variables() {
		return new HashSet<Variable>();
	}

	@Override
	public Expression object() {
		return null;
	}
}
