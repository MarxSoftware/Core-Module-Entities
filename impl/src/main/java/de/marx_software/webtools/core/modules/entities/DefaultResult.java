package de.marx_software.webtools.core.modules.entities;

import de.marx_software.webtools.api.entities.Result;
import java.util.ArrayList;

/**
 *
 * @author marx
 */
public class DefaultResult<T> extends ArrayList<T> implements Result<T> {

	public static final Result EMPTY = new DefaultResult(0, 0, 0);
	private static final long serialVersionUID = 8605787757855134693L;

	private final int totalSize;
	private final int offset;
	private final int limit;

	public DefaultResult(final int totalSize, final int offset, final int limit) {
		super();
		this.totalSize = totalSize;
		this.offset = offset;
		this.limit = limit;
	}

	public int totalSize() {
		return totalSize;
	}

	public int offset() {
		return offset;
	}

	public int limit() {
		return limit;
	}

}
