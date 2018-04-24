package edu.usc.softarch.arcade.util.convert;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import com.google.common.base.Joiner;

public class ExcelToRsfConverter {
	public static void main(String[] args) {
		final String excelFilename = args[0];
		final String rsfFilename = args[1];

		try {
			final InputStream inp = new FileInputStream(excelFilename);
			final Workbook wb = WorkbookFactory.create(inp);
			final Sheet sheet = wb.getSheetAt(0);
			final Set<List<String>> facts = new HashSet<List<String>>();
			final PrintStream origOut = System.out;
			System.setOut(new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
				}
			}));
			for (final Row row : sheet) {
				if (row.getRowNum() == 0) { // Skip header row and column
					continue;
				}
				final List<String> fact = new ArrayList<String>();
				fact.add("contain");
				for (final Cell cell : row) {

					buildFact(row, fact, cell);
					facts.add(fact);
				}
			}
			System.setOut(origOut);

			System.out.println("As RSF facts...");
			System.out.println(Joiner.on("\n").join(facts));

			final FileWriter fw = new FileWriter(rsfFilename);
			final BufferedWriter out = new BufferedWriter(fw);
			for (final List<String> fact : facts) {
				out.write(Joiner.on(" ").join(fact) + "\n");
			}
			out.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final InvalidFormatException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static void buildFact(Row row, List<String> fact, Cell cell) {
		final CellReference cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex());
		System.out.print(cellRef.formatAsString());
		System.out.print(" - ");

		switch (cell.getCellType()) {
		case Cell.CELL_TYPE_STRING:
			final String cellValue = cell.getRichStringCellValue().getString().trim().replaceAll("\\s", "_");
			fact.add(cellValue);
			System.out.println(cellValue);
			break;
		case Cell.CELL_TYPE_NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				System.out.println(cell.getDateCellValue());
			} else {
				System.out.println(cell.getNumericCellValue());
			}
			break;
		case Cell.CELL_TYPE_BOOLEAN:
			System.out.println(cell.getBooleanCellValue());
			break;
		case Cell.CELL_TYPE_FORMULA:
			System.out.println(cell.getCellFormula());
			break;
		default:
			System.out.println();
		}
	}
}
