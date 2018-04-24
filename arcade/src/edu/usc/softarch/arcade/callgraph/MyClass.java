package edu.usc.softarch.arcade.callgraph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import soot.SootClass;

/**
 * @author joshua
 *
 *         Represents a class of the system to be examined
 *
 */
public class MyClass implements Serializable {
	private static final long serialVersionUID = 5575671464833110817L;
	public String packageName;
	public String className;
	HashSet<MyMethod> methods;

	public void addMethod(final MyMethod m) {
		methods.add(m);
	}

	public String methodsToString(final int tabCount) {
		String methodsStr = "";
		int methodCount = 0;
		final Iterator<MyMethod> iter = methods.iterator();
		while (iter.hasNext()) {
			final MyMethod m = iter.next();
			for (int i = 0; i < tabCount; i++) {
				methodsStr += '\t';
			}
			methodsStr += methodCount + ": " + m.toString() + "\n";
			methodCount++;
		}
		return methodsStr;
	}

	public MyClass(final MyClass declaringClass) {
		className = new String(declaringClass.className);
		packageName = new String(declaringClass.packageName);
		methods = new HashSet<MyMethod>(declaringClass.methods);
	}

	public MyClass(final SootClass declaringClass) {
		className = new String(declaringClass.getShortName());
		packageName = new String(declaringClass.getPackageName());
		methods = new HashSet<MyMethod>();
		/*
		 * for (SootMethod sm : declaringClass.getMethods()) { methods.add(new MyMethod(sm)); }
		 */
	}

	@Override
	public boolean equals(final Object o) {
		final MyClass c = (MyClass) o;
		if (className.equals(c.className) && packageName.equals(c.packageName)
		// (this.methods == null ? true : this.methods.equals(c.methods) )
		) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + (className == null ? 0 : className.hashCode());
		hash = 37 * hash + (packageName == null ? 0 : packageName.hashCode());
		// hash = 37 * hash + (this.methods == null ? 0 :
		// this.methods.hashCode());
		return hash;
	}

	@Override
	public String toString() {
		return packageName + "." + className;
	}

	public HashSet<MyMethod> getMethods() {
		return new HashSet<MyMethod>(methods);
	}

}
