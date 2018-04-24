package edu.usc.softarch.arcade.util;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/**
 * Measures the approximate size of an object in memory, given a Class which has
 * a no-argument constructor.
 */
public final class ObjectSizer {

	/**
	 * First and only argument is the package-qualified name of a class which
	 * has a no-argument constructor.
	 */
	public static void main(final String... aArguments) {
		Class<?> theClass = null;
		try {
			theClass = Class.forName(aArguments[0]);
		} catch (final Exception ex) {
			System.err.println("Cannot build a Class object: " + aArguments[0]);
			System.err.println("Use a package-qualified name, and check classpath.");
		}
		final long size = ObjectSizer.getObjectSize(theClass);
		System.out.println("Approximate size of " + theClass + " objects :" + size);
	}

	public static byte[] sizeOf(final Object obj) throws java.io.IOException {
		final ByteArrayOutputStream byteObject = new ByteArrayOutputStream();
		final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteObject);
		objectOutputStream.writeObject(obj);
		objectOutputStream.flush();
		objectOutputStream.close();
		byteObject.close();

		return byteObject.toByteArray();
	}

	/**
	 * Return the approximate size in bytes, and return zero if the class has no
	 * default constructor.
	 *
	 * @param aClass
	 *            refers to a class which has a no-argument constructor.
	 */
	public static long getObjectSize(final Class<?> aClass) {
		long result = 0;

		// if the class does not have a no-argument constructor, then
		// inform the user and return 0.
		try {
			aClass.getConstructor(new Class[] {});
		} catch (final NoSuchMethodException ex) {
			System.err.println(aClass + " does not have a no-argument constructor.");
			return result;
		}

		// this array will simply hold a bunch of references, such that
		// the objects cannot be garbage-collected
		final Object[] objects = new Object[fSAMPLE_SIZE];

		// build a bunch of identical objects
		try {
			// Object throwAway = aClass.newInstance();

			final long startMemoryUse = getMemoryUse();
			for (int idx = 0; idx < objects.length; ++idx) {
				objects[idx] = aClass.newInstance();
			}
			final long endMemoryUse = getMemoryUse();

			final float approximateSize = (endMemoryUse - startMemoryUse) / 100f;
			result = Math.round(approximateSize);
		} catch (final Exception ex) {
			System.err.println("Cannot create object using " + aClass);
		}
		return result;
	}

	// PRIVATE //
	private static int fSAMPLE_SIZE = 100;
	private static long fSLEEP_INTERVAL = 100;

	private static long getMemoryUse() {
		putOutTheGarbage();
		final long totalMemory = Runtime.getRuntime().totalMemory();

		putOutTheGarbage();
		final long freeMemory = Runtime.getRuntime().freeMemory();

		return totalMemory - freeMemory;
	}

	private static void putOutTheGarbage() {
		collectGarbage();
		collectGarbage();
	}

	private static void collectGarbage() {
		try {
			System.gc();
			Thread.currentThread();
			Thread.sleep(fSLEEP_INTERVAL);
			System.runFinalization();
			Thread.currentThread();
			Thread.sleep(fSLEEP_INTERVAL);
		} catch (final InterruptedException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
}
