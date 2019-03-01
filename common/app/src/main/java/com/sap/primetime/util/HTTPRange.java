package com.sap.primetime.util;

public class HTTPRange {
	private long start;
	private long end;
	private long total;

	public HTTPRange(long start, long end, long total) {
		this.start = start;
		this.end = end;
		this.total = total;
	}

	public long getLength() {
		return getEnd() - getStart() + 1;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public long getTotal() {
		return total;
	}

}
