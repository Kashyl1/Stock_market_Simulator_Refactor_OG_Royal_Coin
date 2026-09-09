package com.tradingsimulator.backend.common.error;

public class AppException extends RuntimeException {

	private final transient ErrorCode errorCode;
	private final transient Object[] args;

	public AppException(ErrorCode errorCode, Object... args) {
		super(render(errorCode.messageTemplate(), args));
		this.errorCode = errorCode;
		this.args = args;
	}

	public AppException(ErrorCode errorCode, Throwable cause, Object... args) {
		super(render(errorCode.messageTemplate(), args), cause);
		this.errorCode = errorCode;
		this.args = args;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public Object[] args() {
		return args == null ? new Object[0] : args.clone();
	}

	private static String render(String template, Object[] args) {
		if (args == null || args.length == 0) {
			return template;
		}
		String result = template;
		for (int i = 0; i < args.length; i++) {
			result = result.replace("{" + i + "}", String.valueOf(args[i]));
		}
		return result;
	}
}
