package jol.core;

import java.io.FilterOutputStream;
import java.io.IOException;

import jol.types.basic.Tuple;
import jol.types.exception.UpdateException;
import jol.types.table.Key;
import jol.types.table.ObjectTable;
import jol.types.table.TableName;

/**
 * A table that stores log entries. This
 * table can be referred to from within
 * the OverLog language.
 */
public class Log extends ObjectTable {
	
	public static final TableName TABLENAME = new TableName(GLOBALSCOPE, "log");
	public static final Key PRIMARY_KEY = new Key();
	
	public enum Type{INFO, WARNING, ERROR};
	
	public enum Field{TYPE, MESSAGE};
	public static final Class[] SCHEMA = {
		Enum.class,    // Message type
		String.class   // Log message
	};

	private FilterOutputStream stream;
	
	public Log(Runtime context, FilterOutputStream stream) {
		super(context, TABLENAME, PRIMARY_KEY, SCHEMA);
		this.stream = stream;
	}
	
	@Override
	public boolean insert(Tuple tuple) throws UpdateException {
		String log = "LOGTYPE [" + tuple.value(Field.TYPE.ordinal()) + "], ";
		log       += tuple.value(Field.MESSAGE.ordinal()) + "\n";
		
		try {
			stream.write(log.getBytes());
		} catch (IOException e) {
			throw new UpdateException(e.toString());
		}
		return super.insert(tuple);
	}

}
