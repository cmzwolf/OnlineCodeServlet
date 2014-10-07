package net.ivoa.pdl.utils;

import java.util.ArrayList;
import java.util.List;

public class ErrorDetail {

	public List<String> getParamNames() {
		return paramNames;
	}

	public void setParamNames(List<String> paramNames) {
		this.paramNames = paramNames;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public ErrorDetail(List<String> paramNames, String errorMessage) {
		super();
		this.paramNames = paramNames;
		this.errorMessage = errorMessage;
	}

	public ErrorDetail(String paramName, String errorMessage) {
		super();

		List<String> tempList = new ArrayList<String>();
		tempList.add(paramName);
		this.paramNames = tempList;
		this.errorMessage = errorMessage;
	}

	private List<String> paramNames;
	private String errorMessage;
}
