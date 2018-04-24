package edu.usc.softarch.arcade.callgraph;

import java.io.Serializable;
import java.util.HashSet;

import soot.SootMethod;

/**
 * @author joshua
 *
 *         Represents a class in a system to be examined
 *
 */
public class MyMethod implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -268381565397216273L;
	public String name;
	HashSet<String> params;
	public String retVal;
	public MyClass declaringClass;
	public boolean isPublic;
	public String type;

	public MyMethod(final MyMethod method) {
		name = new String(method.name);
		retVal = new String(method.retVal);
		declaringClass = method.declaringClass;
		params = new HashSet<String>(method.params);
		isPublic = method.isPublic;
	}

	public MyMethod(final SootMethod method) {
		name = new String(method.getName());
		retVal = new String(method.getReturnType().toString());
		params = new HashSet<String>();
		// List<Type> parameterTypes = method.getParameterTypes();
		// for (Type t : parameterTypes) {
		// List<Type> parameterTypes = method.getParameterTypes();
		for (final Object t : method.getParameterTypes()) {
			params.add(new String(t.toString()));
		}
		declaringClass = new MyClass(method.getDeclaringClass());
		isPublic = method.isPublic();
	}

	@Override
	public boolean equals(final Object o) {
		final MyMethod method = (MyMethod) o;
		if (name.equals(method.name) && retVal.equals(method.retVal) && declaringClass.equals(method.declaringClass) && params.equals(method.params) && isPublic == method.isPublic) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + (name == null ? 0 : name.hashCode());
		hash = 37 * hash + (retVal == null ? 0 : retVal.hashCode());
		hash = 37 * hash + (declaringClass == null ? 0 : declaringClass.hashCode());
		hash = 37 * hash + (params == null ? 0 : params.hashCode());
		hash = 37 * hash + (isPublic ? 1 : 0);
		return hash;
	}

	@Override
	public String toString() {
		return "(" + (isPublic ? "public" : "private") + "," + declaringClass.toString() + "." + name + ")";
		// return this.name;
	}

	public HashSet<String> getParams() {
		return new HashSet<String>(params);
	}
}
