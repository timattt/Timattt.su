import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.swing.JFileChooser;

public class Cognitia {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void see(boolean moreColors, boolean drawHidden) throws InterruptedException, IOException, URISyntaxException {
		File parent = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
		File docs = new File(new JFileChooser().getFileSystemView().getDefaultDirectory().toString());
		File srcfile = new File(parent, "RESEARCH_GRAPH.dot");
		File output = new File(docs, "out.dot");
		File available = new File(docs, "RESEARCHED.txt");

		if (!srcfile.exists()) {
			System.err.println("No research graph tree! Searched at " + srcfile.getAbsolutePath());
			return;
		}

		if (!available.exists()) {
			available.createNewFile();
			BufferedWriter wr = new BufferedWriter(new FileWriter(available));
			wr.write("1");
			wr.newLine();
			wr.close();
			System.err.println("No researched file found! It will be created at " + available.getAbsolutePath());
		}

		output.delete();
		output.createNewFile();

		File png = new File("RESEARCH_IMAGE.png");
		png.delete();

		// AVAILABLE parse
		// -------------------------------
		BufferedReader rd = new BufferedReader(new FileReader(available));
		String line = null;
		TreeSet<Integer> available_verts = new TreeSet<Integer>();
		while ((line = rd.readLine()) != null) {
			if (line.length() != 0) {
				available_verts.add(Integer.parseInt(line));
			}
		}
		rd.close();
		// -------------------------------

		BufferedWriter wr = new BufferedWriter(new FileWriter(output));
		rd = new BufferedReader(new FileReader(srcfile));
		line = null;
		String src = "";
		while ((line = rd.readLine()) != null) {
			src += line + "\n";
		}

		String[] divs = src.split("\t");

		LinkedList<Vertex> verts_raw = new LinkedList<Vertex>();

		Vertex curr = null;
		for (int i = 0; i < divs.length; i++) {
			// Vertex NAME
			if (divs[i].contains("[LabelGraphics")) {
				curr = new Vertex();
				curr.name = divs[i - 1];
			}
			if (divs[i].contains("color") && curr.color == null) {
				curr.color = divs[i].substring(divs[i].indexOf('"'), divs[i].lastIndexOf('"')) + '"';
			}
			if (divs[i].contains("label")) {
				if (divs[i].contains("" + '"')) {
					curr.text = divs[i].substring(divs[i].indexOf('"'), divs[i].lastIndexOf('"'))
							.replaceAll("" + (char) 134, "");
				} else {
					curr.text = divs[i].substring(divs[i].indexOf('='), divs[i].lastIndexOf(',')).replace("=", "");
				}
				verts_raw.add(curr);
			}
		}

		Vertex[] verts = new Vertex[verts_raw.size()];
		ArrayList[] g = new ArrayList[verts_raw.size()];

		for (int i = 0; i < g.length; i++) {
			g[i] = new ArrayList<Vertex>();
		}

		for (Vertex v : verts_raw) {
			int id = Integer.parseInt(v.name);
			v.vert_id = id;
			v.text = v.text.replace("" + '"', "").replace("" + '\\', "").replace("\n", "");
			v.name_id = Integer.parseInt(v.text.substring(v.text.indexOf('[') + 1, v.text.indexOf(']')));
			verts[id] = v;
		}

		for (int i = 0; i < divs.length; i++) {
			// Edge
			if (divs[i].contains("->")) {
				String[] divs_ = divs[i].split(" ");
				String v1_raw = divs_[0];
				String v2_raw = divs_[2];

				int v1 = Integer.parseInt(v1_raw);
				int v2 = Integer.parseInt(v2_raw);

				g[v1].add(verts[v2]);
			}
		}

		TreeSet<Integer> to_add = new TreeSet<Integer>();

		for (int from = 0; from < g.length; from++) {
			for (Object to_ : g[from]) {
				Vertex to = (Vertex) to_;
				if (available_verts.contains(verts[from].name_id) && !available_verts.contains(to.name_id)) {
					to_add.add(to.name_id);
				}
			}
		}

		for (int id : to_add) {
			available_verts.add(id);
		}

		wr.write("digraph G {");
		wr.newLine();
		for (Vertex v : verts) {
			if (!available_verts.contains(v.name_id)) {
				if (drawHidden) {
					wr.write("\t" + v.name + " [label=" + '"' + "?" + '"' + ", style=filled, fillcolor=lightgrey];");
				}
			} else {
				String res = "";
				res += ("\t" + v.name + " [label=" + '"' + v.text + '"' + ", style=filled, fillcolor=");
				if (to_add.contains(v.name_id)) {
					if (moreColors && v.color.contains("FFFF00")) {
						res += "lightgreen";
					} else {
						res += "green";
					}
				} else {
					if (moreColors && v.color.contains("FFFF00")) {
						res += "lightcyan";
					} else {
						res += "cyan";
					}
				}
				if (v.color.contains("FFFF00")) {
					res += ", shape=diamond";
				}
				res += ("];");
				wr.write(res);
			}
			wr.newLine();
		}

		for (int from = 0; from < g.length; from++) {
			for (Object to_ : g[from]) {
				Vertex to = (Vertex) to_;
				if (drawHidden
						|| available_verts.contains(verts[from].name_id) && available_verts.contains(to.name_id)) {
					wr.write(from + " -> " + to.vert_id + ";");
					wr.newLine();
				}
			}
		}

		wr.write("}");
		wr.newLine();

		rd.close();
		wr.close();

		Runtime run = Runtime.getRuntime();
		Process pr = run.exec("dot -Tpng out.dot -o RESEARCH_IMAGE.png");
		pr.waitFor();

		Desktop dt = Desktop.getDesktop();
		dt.open(png);

		output.delete();
	}

	public static void unlock(int val) throws IOException, URISyntaxException {
		File docs = new File(new JFileChooser().getFileSystemView().getDefaultDirectory().toString());
		File available = new File(docs, "RESEARCHED.txt");
		if (!available.exists()) {
			available.createNewFile();
		}
		BufferedWriter wr = new BufferedWriter(new FileWriter(available, true));
		wr.newLine();
		wr.write(Integer.toString(val));
		wr.newLine();
		wr.close();
	}

	public static void man(int val) throws IOException, URISyntaxException {
		try {
			Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
			desktop.browse(new URI("/www/timattt.su/Cognitia/mans/" + Integer.toString(val) + ".pdf"));
		} catch (Exception e) {
			System.err.println("No such man!");
		}
	}

	public static void main(String[] args)
			throws IOException, InterruptedException, NumberFormatException, URISyntaxException {
		if (args.length == 0) {
			System.out.println("Cognitia version 0.1");
			return;
		}
		if (args[0].contains("see")) {
			boolean drawHidden = false;
			boolean moreColors = false;
			for (int i = 1; i < args.length; i++) {
				if (args[i].contains("mc")) {
					moreColors = true;
				}
				if (args[i].contains("dh")) {
					drawHidden = true;
				}
			}
			see(moreColors, drawHidden);
		}
		if (args[0].contains("unlock")) {
			if (args.length == 1) {
				System.err.println("too few args!");
				return;
			}
			unlock(Integer.parseInt(args[1]));
		}
		if (args[0].contains("man")) {
			if (args.length == 1) {
				System.err.println("too few args!");
				return;
			}
			man(Integer.parseInt(args[1]));
		}

	}

	static class Vertex {
		String name;
		String color;
		String text;
		int vert_id;
		int name_id;

		@Override
		public String toString() {
			return "Vertex [name=" + name + ", color=" + color + ", text=" + text + ", vert_id=" + vert_id
					+ ", name_id=" + name_id + "]";
		}

	};

}
