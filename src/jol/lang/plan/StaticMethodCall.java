package jol.lang.plan;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xtc.tree.Node;

import jol.types.basic.Tuple;
import jol.types.exception.JolRuntimeException;
import jol.types.exception.PlannerException;
import jol.types.function.TupleFunction;
import jol.types.basic.Schema;

public class StaticMethodCall extends Expression {

	private Class type;

	private Field field;

	private Method method;

	private List<Expression> arguments;

	public StaticMethodCall(Node node, Class type, Method method, List<Expression> arguments) {
		super(node);
		this.type = type;
		this.field = null;
		this.method = method;
		this.arguments = arguments;
	}

	public StaticMethodCall(Node node, Field field, Method method, List<Expression> arguments) {
		super(node);
		this.type = null;
		this.field = field;
		this.method = method;
		this.arguments = arguments;
	}

	public Expression clone() {
		List<Expression> arguments = new ArrayList<Expression>();
		for (Expression arg : this.arguments) {
			arguments.add(arg.clone());
		}
		return this.type == null ? 
				new StaticMethodCall(node(), type, method, arguments) :
			    new StaticMethodCall(node(), field, method, arguments);
	}
	
	@Override
	public String toString() {
		String name = "";
		if (field != null && method != null) {
			name = this.field.getName() + "." + method.getName();
		}
		else if (type != null && method != null) {
			name = type.getName() + "." + method.getName();
		}
		else {
			return name;
		}
		
		if (arguments.size() == 0) {
			return name + "()";
		}
		name += "(" + arguments.get(0).toString();
		for (int i = 1; i < arguments.size(); i++) {
			name += ", " + arguments.get(i);
		}
		return name + ")";
	}

	@Override
	public Class type() {
		return method.getReturnType();
	}

	@Override
	public Set<Variable> variables() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Expression<?> arg : arguments) {
			variables.addAll(arg.variables());
		}
		return variables;
	}

	@Override
	public TupleFunction function(Schema schema) throws PlannerException {
		final List<TupleFunction> argFunctions = new ArrayList<TupleFunction>();
		for (Expression argument : this.arguments) {
			argFunctions.add(argument.function(schema));
		}

		return new TupleFunction() {
			public Object evaluate(Tuple tuple) throws JolRuntimeException {
				Object[] arguments = new Object[StaticMethodCall.this.arguments.size()];
				int index = 0;
				for (TupleFunction argFunction : argFunctions) {
					arguments[index++] = argFunction.evaluate(tuple);
				}
				try {
					return StaticMethodCall.this.method.invoke(null, arguments);
				} catch (Exception e) {
					StringBuilder sb = new StringBuilder();
					sb.append("ERROR: " + e.toString() + ".");
					sb.append("\n Occurred while evaluating static call " + StaticMethodCall.this.toString());
					sb.append("\n Argument values = " + Arrays.toString(arguments));
					Expression args[] = StaticMethodCall.this.arguments.toArray(new Expression[arguments.length]);
					if (args.length > 0) {
						sb.append("\n Argument types = [");
						sb.append(args[0].type());
						for (int i = 1; i < args.length; i++) {
							sb.append(", " + args[i].type());
						}
						sb.append("]");
					}
					throw new JolRuntimeException(sb.toString(), e);
				}
			}

			public Class returnType() {
				return type();
			}
		};
	}
}
