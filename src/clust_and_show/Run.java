package clust_and_show;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import feature_abstract.Feature;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import java.util.Map.Entry;



public class Run {
	//protected static String[] all_features={"look", "screen", "price", "camera", "time", "speed", "battery", "version", "quality", "performance", "storage", "user", "feature", "purchase", "design", "value", "part", "size"};
	protected static String folder="E:/Tsinghua/毕设/project1/remote_service/";
	//protected static String folder="./";
	protected static String product_name="";
	protected static List<String> all_features=new ArrayList<String>();
	protected static HashMap<String,Feature> FEATURE=new HashMap<String,Feature>();
	protected static int feature_num;	
	protected static int dim;
	protected static Integer cid=0;
	protected static double th=0.7;
	protected static double cover=0.5;
	protected static double[][] context;
	protected static DefaultMutableTreeNode ROOT;
	protected static boolean tree_change_flag;
	protected static HashMap<Integer,String> clust_labels=new HashMap<Integer,String>();
	protected static List<HashSet<String>> clusters=new ArrayList<HashSet<String>>();
	protected static HashMap<Integer,HashSet<String>> cid_feature=new HashMap<Integer,HashSet<String>>();
	protected static HashMap<String,Double> IZscore=new HashMap<String,Double>();
	protected static HashMap<String,Double> EZscore=new HashMap<String,Double>();
	protected static HashMap<String,Double> InSim=new HashMap<String,Double>();
	protected static HashMap<String,Double> ExSim=new HashMap<String,Double>();
	protected static DefaultMutableTreeNode tree_prun(DefaultMutableTreeNode root){
		//----the clust with only one child should be replaced by its child----//
		if(root.isLeaf())
			return root;
		boolean flag_change=false;
		do{
			flag_change=false;
			if(root.getChildCount()==1){
				root=(DefaultMutableTreeNode) root.getChildAt(0);
				flag_change=true;
				tree_change_flag=true;
				continue;
			}
			HashSet<DefaultMutableTreeNode> nodes=new HashSet<DefaultMutableTreeNode>();
			for(int k=0;k<root.getChildCount();k++){
				DefaultMutableTreeNode child_node=(DefaultMutableTreeNode) root.getChildAt(k);
				nodes.add(tree_prun(child_node));
			}
			root.removeAllChildren();
			Iterator<DefaultMutableTreeNode> iter=nodes.iterator();
			while(iter.hasNext())
				root.add(iter.next());
		}
		while(flag_change);
		return root;
	}

	protected static HashSet<DefaultMutableTreeNode> whole_prun(DefaultMutableTreeNode root){
		HashSet<DefaultMutableTreeNode> nodes=new HashSet<DefaultMutableTreeNode>();
		if(root.isLeaf())
			nodes.add(root);
		else if(has_leaf_child(root)){
			HashSet<HashSet<DefaultMutableTreeNode>> child_nodes=new HashSet<HashSet<DefaultMutableTreeNode>>();
			Enumeration<TreeNode> children=root.children();
			while(children.hasMoreElements()){
				TreeNode child=children.nextElement();
				child_nodes.add(whole_prun((DefaultMutableTreeNode) child));
			}
			root.removeAllChildren();
			Iterator<HashSet<DefaultMutableTreeNode>> iter=child_nodes.iterator();
			while(iter.hasNext()){
				HashSet<DefaultMutableTreeNode> hs=iter.next();
				for(DefaultMutableTreeNode n:hs)
					root.add(n);
			}
			nodes.add(root);
		}
		else{
			tree_change_flag=true;
			Enumeration<TreeNode> children=root.children();
			while(children.hasMoreElements()){
				TreeNode child=children.nextElement();
				nodes.addAll(whole_prun((DefaultMutableTreeNode) child));
			}
		}
		return nodes;
	}
	protected static DefaultMutableTreeNode whole_prun(DefaultMutableTreeNode root,Integer INT){
		//----the clust with no direct leaf nodes should be replaced by its children----//
		HashSet<DefaultMutableTreeNode> nodes=new HashSet<DefaultMutableTreeNode>();
		Enumeration<TreeNode> enu=root.children();
		while(enu.hasMoreElements())
			nodes.addAll(whole_prun((DefaultMutableTreeNode) enu.nextElement()));
		root.removeAllChildren();
		Iterator<DefaultMutableTreeNode> iter=nodes.iterator();
		while(iter.hasNext())
			root.add(iter.next());
		return root;
	}
	protected static boolean has_leaf_child(TreeNode root){
		Enumeration<TreeNode> enu=root.children();
		while(enu.hasMoreElements()){
			if(enu.nextElement().isLeaf())
				return true;
		}
		return false;
	}
	protected static DefaultMutableTreeNode move_label_from_leaf(DefaultMutableTreeNode root){
		HashSet<String> label_words=new HashSet<String>();
		Enumeration<TreeNode> enu=root.postorderEnumeration();
		while(enu.hasMoreElements()){
			TreeNode t=enu.nextElement();
			if(t.isLeaf())
				continue;
			label_words.add(t.toString());
			System.out.println(t.toString());
		}
		root=move_label_from_leaf(root,label_words);
		return root;
				
	}
	protected static DefaultMutableTreeNode move_label_from_leaf(DefaultMutableTreeNode root,HashSet<String> label_words){
		//----delete those leaves which are label words----//
		if(root.isLeaf())
			return root;
		HashSet<DefaultMutableTreeNode> nodes=new HashSet<DefaultMutableTreeNode>();
		for(int k=0;k<root.getChildCount();k++){
			
			DefaultMutableTreeNode child_node=(DefaultMutableTreeNode) root.getChildAt(k);
			if(child_node.isLeaf()&&label_words.contains(child_node.toString())){
				tree_change_flag=true;
				continue;
			}
			nodes.add(move_label_from_leaf(child_node,label_words));
		}
		root.removeAllChildren();
		Iterator<DefaultMutableTreeNode> iter=nodes.iterator();
		while(iter.hasNext())
			root.add((MutableTreeNode) iter.next());
		return root;
	}
	protected static DefaultMutableTreeNode leaf_prun(DefaultMutableTreeNode root){
		//----the leaf having no slave-master relationship with its brother or parent should be removed----//
		if(root.isLeaf())
			return root;
		String hyper=root.toString();
		HashSet<String> features=new HashSet<String>();
		Enumeration<TreeNode> tnodes=root.preorderEnumeration();
		while(tnodes.hasMoreElements())
			features.add(tnodes.nextElement().toString());
		features.remove(hyper);
		prun_clust(features,hyper);
		HashSet<DefaultMutableTreeNode> nodes=new HashSet<DefaultMutableTreeNode>();
		for(int k=0;k<root.getChildCount();k++){
			DefaultMutableTreeNode child_node=(DefaultMutableTreeNode) root.getChildAt(k);
			if(child_node.isLeaf()&&(!features.contains(child_node.toString()))){
				tree_change_flag=true;
				continue;
			}
			nodes.add(leaf_prun(child_node));
		}
		root.removeAllChildren();
		Iterator<DefaultMutableTreeNode> iter=nodes.iterator();
		while(iter.hasNext())
			root.add((MutableTreeNode) iter.next());
		return root;
	}
	protected static DefaultMutableTreeNode merge_child(DefaultMutableTreeNode root){
		//----- if two brothers have the same name, then merge them to one -----//
		if(root.isLeaf())
			return root;
		HashMap<String, DefaultMutableTreeNode> map=new HashMap<String,DefaultMutableTreeNode>();
		Enumeration<TreeNode> children = root.children();
		while(children.hasMoreElements()){
			TreeNode child=children.nextElement();
			if(!map.containsKey(child.toString()))
				map.put(child.toString(), merge_child((DefaultMutableTreeNode) child));
			else{
				tree_change_flag=true;
				DefaultMutableTreeNode brother=map.get(child.toString());
				Enumeration<TreeNode> nephews=child.children();
				while(nephews.hasMoreElements())
					brother.add((MutableTreeNode) nephews.nextElement());
				map.put(child.toString(), merge_child(brother));
			}
		}
		root.removeAllChildren();
		Iterator<DefaultMutableTreeNode> iter=map.values().iterator();
		while(iter.hasNext())
			root.add(iter.next());
		return root;
	}
	protected static DefaultMutableTreeNode reshape_tree(DefaultMutableTreeNode root){
		Enumeration<DefaultMutableTreeNode> level_two=root.children();
		List<DefaultMutableTreeNode> new_level_two=new ArrayList<DefaultMutableTreeNode>();
		while(level_two.hasMoreElements()){
			DefaultMutableTreeNode node=level_two.nextElement();
			Enumeration<DefaultMutableTreeNode> leaves=node.postorderEnumeration();
			HashSet<String> new_leaves=new HashSet<String>();
			while(leaves.hasMoreElements()){
				String leaf=leaves.nextElement().toString();
				if(leaf.equals(node.toString()))
					continue;
				new_leaves.add(leaf);
			}
			DefaultMutableTreeNode new_node=new DefaultMutableTreeNode(node.toString());
			for(String s:new_leaves)
				new_node.add(new DefaultMutableTreeNode(s));
			new_level_two.add(new_node);
		}
		root.removeAllChildren();
		for(DefaultMutableTreeNode node:new_level_two)
			root.add(node);
		return root;
	}
	public static void main(String[] args){
//		//String method="direct";//"graph";"agglo";"rbr";"bagglo";"br";
//		//Integer clust_num=3;
//		/*if(args.length>0)
//			clust_num=Integer.parseInt(args[0]);
//		if(args.length>1)
//			method=args[1];*/
//		
//		String[] features={"look", "screen", "price", "camera", "time", "speed", "battery", "version", "quality", "performance", "storage", "user", "feature", "purchase", "design", "value", "part", "size"};
//		//String path="E:/Tsinghua/毕设/project1/remote_service/";
//		
//		String[] methods={"rb","rbr","direct","agglo","graph","bagglo"};
//		String[] sims={"cos","corr","dist","jacc"};
//		String[] crfuns={"i1","i2","g1","g1p","h1","h2"/*,"slink","wslink","clink","wclink","upgma"*/};
//		String[] clust_nums={"2","3","4","5"};
//		String result="";
//		String clust_num="3";
//		for(String sim:sims){
//		//for(String method:methods){
//	        //result=result+"\n\nmethod="+method+"\n\n";
//			result=result+"\n\nsims="+sim+"\n\n";
//			for(String crfun:crfuns){
//			//for(String clust_num:clust_nums){
//				//result=result+"clust_no = "+clust_num+'\n';
//				result=result+"crfun = "+crfun+'\n';
//				
//				//String command="vcluster.exe -clmethod="+method+" "+"Input_Matrix.txt "+clust_num;
//				//String command="vcluster.exe -clmethod=graph -sim="+sim+" "+"Input_Matrix.txt "+clust_num;
//				String command="vcluster.exe -clmethod=direct -sim="+sim+" -crfun="+crfun+" "+"Input_Matrix.txt "+clust_num;
//				clusters.clear();
//				for(int k=0;k<Integer.parseInt(clust_num);k++){
//					HashSet<String> hs=new HashSet<String>();
//					clusters.add(hs);
//				}
//				CommandUtil util = new CommandUtil();
//		        util.executeCommand(command);
//		        printList(util.getStdoutList());
//		        System.out.println("--------------------");
//		        printList(util.getErroroutList());
//		        
//		        String outfile="Input_Matrix.txt.clustering."+clust_num;
//		        try{
//		        	FileReader fr=new FileReader(outfile);
//		        	BufferedReader bufr=new BufferedReader(fr);
//		        	String s=null;
//		        	int i=0;
//		        	int k=0;
//		        	int clust_id;
//		        	while((clust_id=bufr.read())!=-1){
//		        		if(i%3==0){
//		        			clust_id=clust_id-(int) '0';
//		        			clusters.get(clust_id).add(features[k]);
//		        			k++;
//		        			//feature_clust.put(iter.next().name, clust_id);
//		        		}
//		        		i++;
//		        	}
//		        	bufr.close();
//		        	fr.close();
//		        }
//		        catch(Exception e){
//		        	e.printStackTrace();
//		        }
//		        for(Integer k=0;k<Integer.parseInt(clust_num);k++){
//		        	result=result+"clust "+k.toString()+":";
//		        	Iterator<String> it=clusters.get(k).iterator();
//		        	while(it.hasNext())
//		        		result=result+" "+it.next();
//		        	result+='\n';
//		        }
//			}
//		}
//
//        System.out.println(result);
//        try{
//        	//FileWriter fw=new FileWriter("clust_result_method_num.txt");
//        	//FileWriter fw=new FileWriter("clust_result_graph_sim_num.txt");
//        	FileWriter fw=new FileWriter("clust_result_direct_sim_crfun_3.txt");
//			BufferedWriter bufw=new BufferedWriter(fw);
//			bufw.write(result);
//			bufw.close();
//			fw.close();
//        }
//        catch(Exception e){
//        	e.printStackTrace();
//        }
		//----二分层次聚类----//
		String inpath=folder;//+"laptop/processdata_8000/";
		bisection(inpath);
//		do{
//			tree_change_flag=false;
//			ROOT=tree_prun(ROOT);
//			ROOT=whole_prun(ROOT,1);
//			ROOT=move_label_from_leaf(ROOT);
//			ROOT=whole_prun(ROOT,1);
//			ROOT=leaf_prun(ROOT);
//			ROOT=merge_child(ROOT);
//			ROOT=reshape_tree(ROOT);
//		}
//		while(tree_change_flag);
		write_knowledge_base(inpath);
		show_tree();
    }
	public static void show_tree(){
		 JFrame f = new JFrame("JTreeDemo");
	        JTree T=new JTree(ROOT);
	        f.add(T);
	        JScrollPane scrollPane_2 = new JScrollPane();
	        f.getContentPane().add(scrollPane_2, BorderLayout.CENTER);
	        f.setSize(900, 900);
	        scrollPane_2.setViewportView(T);
	        f.setVisible(true);

	        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public static void bisection(String inpath){
		//----二分层次聚类----//
		try{
			//----读取语境向量----//
			FileReader fr=new FileReader(inpath+"context_vecs.txt");
			BufferedReader br=new BufferedReader(fr);
			String s;
			s=br.readLine();
			feature_num=Integer.parseInt(s.split(" ")[0]);
			dim=feature_num+1;
			context=new double[feature_num][dim];//语境向量
			int n=0;
			while((s=br.readLine())!=null){
				String[] ss=s.split(" ");
				for(int m=0;m<dim;m++)
					context[n][m]=Integer.parseInt(ss[m]);
				n++;
			}
//			normalize();
			br.close();
			fr.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		read_features(inpath);//读取属性
		get_product_name(inpath);//读取产品名称（作为属性树根结点）
		ROOT=new DefaultMutableTreeNode(product_name);
		bis_split(inpath,all_features,ROOT,product_name);//二分聚类
		get_hypernym();//抽取标签词（父节点名称）
		//----输出结果----//
		String result="";
		Iterator<Entry<Integer,HashSet<String>>> iter=cid_feature.entrySet().iterator();
		while(iter.hasNext()){
			Entry<Integer,HashSet<String>> ent=iter.next();
			result=result+"cid = "+ent.getKey().toString()+" hypernym = "+clust_labels.get(ent.getKey())+" ISim = "+compute_ISim(new ArrayList<String>(ent.getValue()))+"\n";
			Iterator<String> it=ent.getValue().iterator();
			while(it.hasNext())
				result=result+it.next()+' ';
			result+='\n';
		}
		try{
			FileWriter fw=new FileWriter(inpath+"result_direct_cos_bisc.txt");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(result);
			bw.close();
			fw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void bis_split(String inpath,List<String> features,DefaultMutableTreeNode root,String hyper){
		//----以root为根结点，对features中的结点进行二分聚类----//
		if(compute_ISim(features)>=th||features.size()<2){
			HashSet<String> feature_set=new HashSet<String>(features);
			//prun_clust(feature_set ,hyper);
			if(feature_set.isEmpty())
				root.removeFromParent();
			if(feature_set.size()==1){
				root.setUserObject(feature_set.iterator().next());
				return;
			}
			for(String f:feature_set)
				root.add(new DefaultMutableTreeNode(f));
			return;
		}
		write_input_matrix(inpath,features);
		String command=folder+"vcluster.exe -clmethod=direct -sim=cos -rowmodel=maxtf "+inpath+"Input_Matrix_"+cid.toString()+".txt 2";
		CommandUtil util = new CommandUtil();
        util.executeCommand(command);
        printList(util.getStdoutList());
        System.out.println("--------------------");
        printList(util.getErroroutList());
        String outfile=inpath+"Input_Matrix_"+cid.toString()+".txt.clustering.2";
        HashSet<String> hs1=new HashSet<String>();
        HashSet<String> hs2=new HashSet<String>();
        cid_feature.put(cid, hs1);
        cid_feature.put(cid+1, hs2);
        try{
        	FileReader fr=new FileReader(outfile);
        	BufferedReader bufr=new BufferedReader(fr);
        	int i=0;
        	int k=0;
        	int clust_id;
        	while((clust_id=bufr.read())!=-1){
        		if(i%3==0){
        			clust_id=clust_id-(int) '0'+cid;
        			cid_feature.get(clust_id).add(features.get(k));
        			k++;
        			//feature_clust.put(iter.next().name, clust_id);
        		}
        		i++;
        	}
        	bufr.close();
        	fr.close();
        }
        catch(Exception e){
        	e.printStackTrace();
        }
//        String hyper1=hypernym_zscore(cid_feature.get(cid));
//        String hyper1=hypernym_overlap(cid_feature.get(cid));
        String hyper1=hypernym_sm(cid_feature.get(cid));
        DefaultMutableTreeNode node1=new DefaultMutableTreeNode(hyper1);
//        String hyper2=hypernym_zscore(cid_feature.get(cid));
//        String hyper2=hypernym_overlap(cid_feature.get(cid+1));
        String hyper2=hypernym_sm(cid_feature.get(cid+1));
        DefaultMutableTreeNode node2=new DefaultMutableTreeNode(hyper2);
        root.add(node1);
        root.add(node2);
        List<String> features1=new ArrayList<String>(cid_feature.get(cid));//[cid_feature.get(cid).size()];
        List<String> features2=new ArrayList<String>(cid_feature.get(cid+1));//String[cid_feature.get(cid+1).size()];
     
        cid+=2;
        bis_split(inpath,features1,node1,hyper1);
        bis_split(inpath,features2,node2,hyper2);
	}
	public static int get_id(String f){
		for(int m=0;m<all_features.size();m++){
			if(all_features.get(m).equals(f))
				return m;
		}
		return -1;
	}
	public static void normalize(){
		for(int k=0;k<feature_num;k++){
			double  sum=0;
			double max=0;
			for(int i=0;i<dim;i++){

				context[k][i]=Math.log(context[k][i]);
//				context[k][i]=Math.sqrt(context[k][i]);
				sum+=context[k][i];
				if(max<context[k][i])
					max=context[k][i];
			}
			for(int i=0;i<dim;i++){
//				context[k][i]/=sum;
//				context[k][i]=0.5+context[k][i]/max;
//				context[k][i]=Math.log(context[k][i]);
//				context[k][i]=Math.sqrt(context[k][i]);
			}
		}
	}
	public static void write_input_matrix(String inpath,List<String> features){
		try{
			FileWriter fw=new FileWriter(inpath+"Input_Matrix_"+cid.toString()+".txt");
			BufferedWriter bufw=new BufferedWriter(fw);
			bufw.write(features.size()+" "+dim+'\n');		
			for(int k=0;k<features.size();k++){
				int id=get_id(features.get(k));
				for(int m=0;m<dim;m++)
					bufw.write(String.valueOf(context[id][m])+' ');
				bufw.write('\n');
			}
			bufw.close();
			fw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public static double compute_ISim(List<String> features){
		int N=features.size();
		double ISim=0;
		for(int k=0;k<N;k++){
			int id1=get_id(features.get(k));
			for(int i=0;i<N;i++){
				if(i==k)
					continue;
				int id2=get_id(features.get(i));
				ISim+=compute_sim(context[id1],context[id2]);
			}
		}
		ISim=ISim/(N*(N-1));
		return ISim;
	}
	public static double compute_sim(double[] vec1,double[] vec2){
		double prod=0,v1=0,v2=0;
		for(int k=0;k<dim;k++){
			v1+=vec1[k]*vec1[k];
			v2+=vec2[k]*vec2[k];
			prod+=vec1[k]*vec2[k];
		}
		v1=Math.sqrt(v1);
		v2=Math.sqrt(v2);
		prod=Math.abs(prod)/(v1*v2);
		return prod;
	}
	public static void get_hypernym(){
		Iterator<Entry<Integer,HashSet<String>>> iter=cid_feature.entrySet().iterator();
		while(iter.hasNext()){
			Entry<Integer,HashSet<String>> ent=iter.next();
//			clust_labels.put(ent.getKey(), hypernym_zscore(ent.getValue()));
//			clust_labels.put(ent.getKey(), hypernym_overlap(ent.getValue()));
			clust_labels.put(ent.getKey(), hypernym_sm(ent.getValue()));
		}
	}
	
	public static String hypernym_sm(HashSet<String> features){
		
		HashMap<String,Integer> master_candidate=new HashMap<String,Integer>();
		for(String f:features){
			Feature feature=FEATURE.get(f);
			Iterator<Entry<String,Integer>> iter=feature.master_feature_union.entrySet().iterator();
			while(iter.hasNext()){
				Entry<String,Integer> ent=iter.next();
				String master=ent.getKey();
				if(!master_candidate.containsKey(master))
					master_candidate.put(master, 0);
				Integer freq=master_candidate.get(master);
				freq+=ent.getValue();//*feature.freq;
				master_candidate.put(master, freq);
			}
		}
		List<HashMap.Entry<String, Integer>> masters =
			    new ArrayList<HashMap.Entry<String, Integer>>(master_candidate.entrySet());
		Collections.sort(masters, new Comparator<Map.Entry<String, Integer>>() { 
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String,Integer> o2) {      
				return (o2.getValue() - o1.getValue()); 
				//return (o1.getKey()).toString().compareTo(o2.getKey());
			}
		}); 
		if(masters.size()<2)
			return hypernym_overlap(features);
		if(masters.get(0).getKey().equals(product_name))
			return masters.get(1).getKey();
		return masters.get(0).getKey();
	}
	public static String hypernym_overlap(HashSet<String> features){
		double[] overlap_vec=new double[dim];
		for(String f:features){
			int id=get_id(f);
			for(int k=0;k<dim;k++){
				if(context[id][k]>0)
					overlap_vec[k]+=1;
			}
		}
		for(int k=0;k<dim;k++){
			if(overlap_vec[k]<(features.size()*cover))//||clust_labels.containsValue(all_features.get(k)))
				overlap_vec[k]=0;
			else
				overlap_vec[k]=1;
		}
		for(String f:features){
			int id=get_id(f);
			for(int k=0;k<dim;k++){
				if(context[id][k]>0&&overlap_vec[k]>0)
					overlap_vec[k]+=context[id][k];
			}
		}
		double max_value=overlap_vec[0];
		for(int k=0;k<dim;k++){
			if(overlap_vec[k]>max_value)
				max_value=overlap_vec[k];
		}
		String s="";
		for(int k=1;k<dim;k++){
			if(overlap_vec[k]==max_value){
				s=s+all_features.get(k-1)+" ";
			}
		}
		s=s.substring(0, s.length()-1);
		/*s=s+"="+String.valueOf(max_value-1)+" ";
		for(int k=1;k<dim;k++){
			if(overlap_vec[k]>0){
				s=s+all_features.get(k-1)+"="+String.valueOf(overlap_vec[k]-1)+" ";
			}
		}*/
		
		return s;
	}

	public static void prun_clust(HashSet<String> features,String hypernym){
		List<String> to_remove=new ArrayList<String>();
		for(String s:features){
			Feature f=FEATURE.get(s);
			HashSet<String> slave=new HashSet<String>();
			HashSet<String> master=new HashSet<String>();
			
			slave.addAll(f.slave_feature_union.keySet());
			master.addAll(f.master_feature_union.keySet());
			
//			slave.addAll(f.slave_feature_intsec.keySet());
//			master.addAll(f.master_feature_intsec.keySet());
			
//			slave.addAll(f.slave_feature_comp.keySet());
//			master.addAll(f.master_feature_comp.keySet());
			
//			slave.addAll(f.slave_feature_of.keySet());
//			master.addAll(f.master_feature_of.keySet());
			String[] hyp=hypernym.split(" ");
			if(/*slave.contains(hyp[0])||*/master.contains(hyp[0].toString()))
				continue;
			slave.retainAll(features);
			if(!slave.isEmpty())
				continue;
			master.retainAll(features);
			if(!master.isEmpty())
				continue;
			to_remove.add(s);
		}
		for(String s:to_remove)
			features.remove(s);
		
	}

	public static String hypernym_zscore(HashSet<String> features){
		compute_zscore(features);
		double max_value=Double.NEGATIVE_INFINITY;
		String hyp = null;
		Iterator<String> it=features.iterator();
		while(it.hasNext()){
			String s=it.next();
			if(fracfun(s)>max_value){
				max_value=fracfun(s);
				hyp=s;
			}
		}
		return hyp;
	}
	public static double fracfun(String s){
		return IZscore.get(s);
//		return (1-EZscore.get(s));
//		return (IZscore.get(s)-EZscore.get(s));
//		return (IZscore.get(s)/EZscore.get(s));
	}
	public static void compute_zscore(HashSet<String> features){
		IZscore.clear();
		EZscore.clear();
		compute_IS_ES(features);
		double ui=0,ue=0;
		int n=0;
		for(String f:features){
			ui+=InSim.get(f);
			ue+=ExSim.get(f);
			n++;
		}
		ui=ui/n;
		ue=ue/n;
		double sgmi=0,sgme=0;
		for(String f:features){
			sgmi+=(InSim.get(f)-ui)*(InSim.get(f)-ui);
			sgme+=(ExSim.get(f)-ue)*(ExSim.get(f)-ue);
		}
		sgmi=Math.sqrt(sgmi/n);
		sgme=Math.sqrt(sgme/n);
		for(String f:features){
			IZscore.put(f, (InSim.get(f)-ui)/sgmi);
			EZscore.put(f,(ExSim.get(f)-ue)/sgme);
		}
//		Iterator<Entry<Integer,HashSet<String>>> iter=cid_feature.entrySet().iterator();
//		while(iter.hasNext()){
//			Entry<Integer,HashSet<String>> ent=iter.next();
//			double ui=0,ue=0;
//			int n=0;
//			for(String f:ent.getValue()){
//				ui+=InSim.get(f);
//				ue+=ExSim.get(f);
//				n++;
//			}
//			ui=ui/n;
//			ue=ue/n;
//			double sgmi=0,sgme=0;
//			for(String f:ent.getValue()){
//				sgmi+=(InSim.get(f)-ui)*(InSim.get(f)-ui);
//				sgme+=(ExSim.get(f)-ue)*(ExSim.get(f)-ue);
//			}
//			sgmi=Math.sqrt(sgmi/n);
//			sgme=Math.sqrt(sgme/n);
//			for(String f:ent.getValue()){
//				IZscore.put(f, (InSim.get(f)-ui)/sgmi);
//				EZscore.put(f,(ExSim.get(f)-ue)/sgme);
//			}
//		}
	}
	public static void compute_IS_ES(HashSet<String> features){
		InSim.clear();
		ExSim.clear();
		HashSet<String> exfeatures=new HashSet<String>(all_features);
		exfeatures.removeAll(features);
		for(String f:features){
			double IS=compute_S(features,f);
			InSim.put(f,IS);
			double ES=compute_S(exfeatures,f);
			ExSim.put(f,ES);
		}
//		Iterator<Entry<Integer,HashSet<String>>> iter=cid_feature.entrySet().iterator();
//		while(iter.hasNext()){
//			Entry<Integer,HashSet<String>> ent=iter.next();
//			for(String f:ent.getValue()){
//				double IS=compute_S(ent.getValue(),f);
//				InSim.put(f, IS);
//			}
//		}
//		
//		HashSet<String> union=new HashSet<String>();
//		iter=cid_feature.entrySet().iterator();
//		while(iter.hasNext())
//			union.addAll(iter.next().getValue());
//		
//		iter=cid_feature.entrySet().iterator();
//		while(iter.hasNext()){
//			Entry<Integer,HashSet<String>> ent=iter.next();
//			HashSet<String> ex=new HashSet<String>();
//			ex.addAll(union);
//			ex.removeAll(ent.getValue());
//			for(String f:ent.getValue()){
//				double ES=compute_S(ex,f);
//				ExSim.put(f, ES);
//			}
//		}
		
	}
	public static double compute_S(HashSet<String> features,String f){
		int id1=get_id(f);
		double S=0;
		int n=0;
		for(String f2:features){
			/*if(f.equals(f2))
				continue;*/
			int id2=get_id(f2);
			S+=compute_sim(context[id1],context[id2]);
			n++;
		}
		S=S/n;
		return S;
	}
	
	public static void write_knowledge_base(String inpath){
		try{
			FileWriter fw=new FileWriter(inpath+"aspect_tree.txt");
			BufferedWriter bufw=new BufferedWriter(fw);
			String s=weight_compute();
			bufw.write(s);
			bufw.close();
			fw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public static String weight_compute(){
		String s="";
		s=s+ROOT.toString()+'\t'+"1.0\n";
		Enumeration<DefaultMutableTreeNode> enu=ROOT.children();
		double sum=0;
		while(enu.hasMoreElements())
			sum+=FEATURE.get(enu.nextElement().toString()).freq;
		enu=ROOT.children();
		while(enu.hasMoreElements()){
			DefaultMutableTreeNode node=enu.nextElement();
			s=s+node.toString()+'\t'+node.getParent().toString()+'\t'+((double)FEATURE.get(node.toString()).freq)/sum+'\n';
		}
		enu=ROOT.children();
		while(enu.hasMoreElements())
			s=s+weight_compute(enu.nextElement());
		return s;
	}
	public static String weight_compute(DefaultMutableTreeNode root){
		String s="";
		Enumeration<DefaultMutableTreeNode> enu=root.children();
		double sum=0;
		while(enu.hasMoreElements())
			sum+=FEATURE.get(enu.nextElement().toString()).freq;
		enu=root.children();
		while(enu.hasMoreElements()){
			DefaultMutableTreeNode node=enu.nextElement();
			s=s+node.toString()+'\t'+node.getParent().toString()+'\t'+((double)FEATURE.get(node.toString()).freq)/sum+'\n';
		}
		enu=root.children();
		while(enu.hasMoreElements())
			s=s+weight_compute(enu.nextElement());
		return s;
	}
 	public static void read_features(String inpath){
		try{
			FileReader fr=new FileReader(inpath+"Row_labels.txt");
			BufferedReader bufr=new BufferedReader(fr);
			String s;
			while((s=bufr.readLine())!=null)
				all_features.add(s);
			bufr.close();
			fr.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		try{
			File file=new File(inpath+"ALL_FEATURE.dat");
			FileInputStream fi=new FileInputStream(file);
			ObjectInputStream oi=new ObjectInputStream(fi);
			try{
				while(true){
					Feature f=(Feature) oi.readObject();
					FEATURE.put(f.feature, f);
				}
			}
			catch(EOFException e){
				oi.close();
				fi.close();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
    public static void get_product_name(String inpath){
    	try{
			File file=new File(inpath+"ALL_FEATURE.dat");
			FileInputStream fi=new FileInputStream(file);
			ObjectInputStream oi=new ObjectInputStream(fi);
			try{
				Feature f=(Feature) oi.readObject();
				product_name=f.feature;
				oi.close();
				fi.close();
			}
			catch(Exception e){
				oi.close();
				fi.close();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
    }
 	public static void printList(List<String> list){
        for (String string : list) {
            System.out.println(string);
        }
    }
}
