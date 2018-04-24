package edu.usc.softarch.arcade.util.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.util.Animator;

public class TreeGraphGenerator extends JApplet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * the graph
	 */
	Tree<String, Integer> graph;

	Factory<DirectedGraph<String, Integer>> graphFactory = new Factory<DirectedGraph<String, Integer>>() {

		@Override
		public DirectedGraph<String, Integer> create() {
			return new DirectedSparseMultigraph<String, Integer>();
		}
	};

	Factory<Tree<String, Integer>> treeFactory = new Factory<Tree<String, Integer>>() {

		@Override
		public Tree<String, Integer> create() {
			return new DelegateTree<String, Integer>(graphFactory);
		}
	};

	Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i = 0;

		@Override
		public Integer create() {
			return i++;
		}
	};

	Factory<String> vertexFactory = new Factory<String>() {
		int i = 0;

		@Override
		public String create() {
			return "V" + i++;
		}
	};

	/**
	 * the visual component and renderer for the graph
	 */
	VisualizationViewer<String, Integer> vv;

	VisualizationServer.Paintable rings;

	String root;

	TreeLayout<String, Integer> treeLayout;

	RadialTreeLayout<String, Integer> radialLayout;

	public TreeGraphGenerator() {

		// create a simple graph for the demo
		graph = new DelegateTree<String, Integer>();

		createTree();

		buildViewer();
	}

	public TreeGraphGenerator(final Tree<String, Integer> tree) {
		graph = tree;
		buildViewer();

	}

	private void buildViewer() {
		treeLayout = new TreeLayout<String, Integer>(graph);
		radialLayout = new RadialTreeLayout<String, Integer>(graph);
		radialLayout.setSize(new Dimension(600, 600));
		vv = new VisualizationViewer<String, Integer>(treeLayout, new Dimension(600, 600));
		vv.setBackground(Color.white);
		vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		// add a listener for ToolTips
		vv.setVertexToolTipTransformer(new ToStringLabeller());
		vv.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
		rings = new Rings();

		final Container content = getContentPane();
		final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
		content.add(panel);

		final DefaultModalGraphMouse<Object, Object> graphMouse = new DefaultModalGraphMouse<Object, Object>();

		vv.setGraphMouse(graphMouse);

		final JComboBox<?> modeBox = graphMouse.getModeComboBox();
		modeBox.addItemListener(graphMouse.getModeListener());
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);

		final ScalingControl scaler = new CrossoverScalingControl();

		final JButton plus = new JButton("+");
		plus.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				scaler.scale(vv, 1.1f, vv.getCenter());
			}
		});
		final JButton minus = new JButton("-");
		minus.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				scaler.scale(vv, 1 / 1.1f, vv.getCenter());
			}
		});

		final JToggleButton radial = new JToggleButton("Radial");
		radial.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {

					final LayoutTransition<String, Integer> lt = new LayoutTransition<String, Integer>(vv, treeLayout, radialLayout);
					final Animator animator = new Animator(lt);
					animator.start();
					vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
					vv.addPreRenderPaintable(rings);
				} else {
					final LayoutTransition<String, Integer> lt = new LayoutTransition<String, Integer>(vv, radialLayout, treeLayout);
					final Animator animator = new Animator(lt);
					animator.start();
					vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
					vv.removePreRenderPaintable(rings);
				}
				vv.repaint();
			}
		});

		final JPanel scaleGrid = new JPanel(new GridLayout(1, 0));
		scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));

		final JPanel controls = new JPanel();
		scaleGrid.add(plus);
		scaleGrid.add(minus);
		controls.add(radial);
		controls.add(scaleGrid);
		controls.add(modeBox);

		content.add(controls, BorderLayout.SOUTH);
	}

	class Rings implements VisualizationServer.Paintable {

		Collection<Double> depths;

		public Rings() {
			depths = getDepths();
		}

		private Collection<Double> getDepths() {
			final Set<Double> depths = new HashSet<Double>();
			final Map<String, PolarPoint> polarLocations = radialLayout.getPolarLocations();
			for (final String v : graph.getVertices()) {
				final PolarPoint pp = polarLocations.get(v);
				depths.add(pp.getRadius());
			}
			return depths;
		}

		@Override
		public void paint(final Graphics g) {
			g.setColor(Color.lightGray);

			final Graphics2D g2d = (Graphics2D) g;
			final Point2D center = radialLayout.getCenter();

			final Ellipse2D ellipse = new Ellipse2D.Double();
			for (final double d : depths) {
				ellipse.setFrameFromDiagonal(center.getX() - d, center.getY() - d, center.getX() + d, center.getY() + d);
				final Shape shape = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).transform(ellipse);
				g2d.draw(shape);
			}
		}

		@Override
		public boolean useTransform() {
			return true;
		}
	}

	/**
	 *
	 */
	private void createTree() {

		final int maxChildren = 5;
		final int maxHeight = 5;

		final Random random = new Random();

		final String currVertex = vertexFactory.create();
		graph.addVertex(currVertex);

		growTree(maxChildren, maxHeight, random, currVertex);

		final String dirStr = "data" + File.separator + "generated" + File.separator;
		final String graphMlFilename = "generated_tree_" + maxChildren + "mc_" + maxHeight + "mh.graphml";
		final String rsfFilename = "generated_tree_" + maxChildren + "mc_" + maxHeight + "mh.rsf";
		final File dir = new File(dirStr);
		dir.mkdirs();
		final GraphMLWriter<String, Integer> graphMLWriter = new GraphMLWriter<String, Integer>();
		final Collection<Integer> edges = graph.getEdges();
		try {
			graphMLWriter.save(graph, new FileWriter(dirStr + graphMlFilename));

			final FileWriter fileWriter = new FileWriter(dirStr + rsfFilename);
			final BufferedWriter out = new BufferedWriter(fileWriter);
			for (final Integer edge : edges) {
				out.write("depends " + graph.getSource(edge) + " " + graph.getDest(edge) + "\n");
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private void growTree(final int maxChildren, final int maxHeight, final Random random, final String currVertex) {
		createChildren(maxChildren, random, currVertex);
		final Collection<String> children = graph.getChildren(currVertex);
		if (maxHeight > graph.getHeight()) {
			for (final String child : children) {
				growTree(maxChildren, maxHeight, random, child);
			}
		}
	}

	private void createChildren(final int maxChildren, final Random random, final String currVertex) {

		for (int i = 0; i < maxChildren; i++) {
			if (random.nextInt(100) > 50) {
				graph.addEdge(edgeFactory.create(), currVertex, vertexFactory.create());
			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		final JFrame frame = new JFrame();
		final Container content = frame.getContentPane();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		content.add(new TreeGraphGenerator());
		frame.pack();
		frame.setVisible(true);

		/*
		 * for (int i=0;i<numEntities;i++) { for (int j=0;j<numEntities;j++) {
		 * depMatrix[i][j] = random.nextInt(2); } }
		 * 
		 * for (int i=0;i<numEntities;i++) { for (int j = 0; j < numEntities;
		 * j++) { System.out.print(depMatrix[i][j] + " "); }
		 * System.out.println(); }
		 * 
		 * 
		 * for (int i=0;i<numEntities;i++) { for (int j=0;j<numEntities;j++) {
		 * if (depMatrix[i][j] == 1) { System.out.println("depends e" + i + " e"
		 * + j); } } }
		 * 
		 * 
		 * 
		 * try { String dirStr = "data" + File.separator + "generated" +
		 * File.separator; File dir = new File(dirStr); dir.mkdirs(); FileWriter
		 * fstream = new FileWriter(dirStr + "generated_deps_" + numEntities +
		 * ".rsf"); BufferedWriter out = new BufferedWriter(fstream);
		 * 
		 * for (int i=0;i<numEntities;i++) { for (int j=0;j<numEntities;j++) {
		 * if (depMatrix[i][j] == 1) { out.write("depends e" + i + ".c e" + j +
		 * ".c\n"); } } }
		 * 
		 * out.close(); } catch (IOException e) {
		 * e.printStackTrace();System.exit(-1); }
		 */

	}

}
