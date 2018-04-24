package mojo;

import java.io.File;

import edu.usc.softarch.arcade.util.FileUtil;

public class MoJo {

	public static void main(String[] args) {
		try {

			MoJoCalculator mjc;
			if (args.length < 2 || args.length > 4) {
				showerrormsg();
			}
			final File sourceFile = FileUtil.checkFile(args[0], false, false);
			final File targetFile = FileUtil.checkFile(args[1], false, false);
			File relFile = null;
			if (args.length > 2) {
				/* -m+ indicates single direction MoJoPlus */
				if (args[2].equalsIgnoreCase("-m+")) {
					mjc = new MoJoCalculator(sourceFile, targetFile, relFile);
					System.out.println(mjc.mojoplus());
				} else
					/* -b+ indicates double direction MoJoPlus */
					if (args[2].equalsIgnoreCase("-b+")) {
						mjc = new MoJoCalculator(sourceFile, targetFile, relFile);
						final long one = mjc.mojoplus();
						mjc = new MoJoCalculator(targetFile, sourceFile, relFile);
						final long two = mjc.mojoplus();
						System.out.println(Math.min(one, two));
					} else
						/* -b indicates double direction MoJo */
						if (args[2].equalsIgnoreCase("-b")) {
							mjc = new MoJoCalculator(sourceFile, targetFile, relFile);
							final long one = mjc.mojo();
							mjc = new MoJoCalculator(targetFile, sourceFile, relFile);
							final long two = mjc.mojo();
							System.out.println(Math.min(one, two));
						} else
							/* -fm asks for MoJoFM value */
							if (args[2].equalsIgnoreCase("-fm")) {
								mjc = new MoJoCalculator(sourceFile, targetFile, relFile);
								System.out.println(mjc.mojofm());
							} else
				/* -fm asks for MoJoEvo value */
				if (args[2].equalsIgnoreCase("-ev")) {
					mjc = new MoJoCalculator(sourceFile, targetFile, relFile);
					System.out.println(mjc.mojoev());
				} else
									// -e indicates EdgeMoJo (requires extra argument)
									if (args[2].equalsIgnoreCase("-e")) {
										if (args.length == 4) {
											relFile = FileUtil.checkFile(args[3], false, false);
											mjc = new MoJoCalculator(sourceFile, targetFile, relFile);
											System.out.println(mjc.edgemojo());
										} else {
											showerrormsg();
										}
									} else {
										showerrormsg();
									}

			} else {
				mjc = new MoJoCalculator(sourceFile, targetFile, relFile);
				System.out.println(mjc.mojo());
			}
		} catch (final RuntimeException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getMessage());
			System.out.println(e.getMessage());
			System.out.println(e.getMessage());
			System.out.println(e.getMessage());
			System.out.println(e.getMessage());
		}
	}

	private static void showerrormsg() {
		System.out.println("");
		System.out.println("Please use one of the following:");
		System.out.println("");
		System.out.println("java mojo.MoJo a.rsf b.rsf");
		System.out.println("  calculates the one-way MoJo distance from a.rsf to b.rsf");
		System.out.println("java mojo.MoJo a.rsf b.rsf -fm");
		System.out.println("  calculates the MoJoFM distance from a.rsf to b.rsf");
		System.out.println("java mojo.MoJo a.rsf b.rsf -b");
		System.out.println("  calculates the two-way MoJo distance between a.rsf and b.rsf");
		System.out.println("java mojo.MoJo a.rsf b.rsf -e r.rsf");
		System.out.println("  calculates the EdgeMoJo distance between a.rsf and b.rsf");
		System.out.println("java mojo.MoJo a.rsf b.rsf -m+");
		System.out.println("  calculates the one-way MoJoPlus distance from a.rsf to b.rsf");
		System.out.println("java mojo.MoJo a.rsf b.rsf -b+");
		System.out.println("  calculates the two-way MoJoPlus distance between a.rsf and b.rsf");
		System.exit(0);
	}

}
