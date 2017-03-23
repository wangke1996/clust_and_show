package clust_and_show;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import java.util.Map.Entry;



public class Run {
	//protected static String[] all_features={"look", "screen", "price", "camera", "time", "speed", "battery", "version", "quality", "performance", "storage", "user", "feature", "purchase", "design", "value", "part", "size"};
	//protected static String folder="E:/Tsinghua/±œ…Ë/project1/remote_service";
	protected static String folder=".";
	protected static List<String> all_features=new ArrayList<String>();
	protected static int feature_num=53;	
	protected static int dim=54;
	protected static Integer cid=0;
	protected static double th=0.68;
	protected static double cover=0.5;
	protected static double[][] context=new double[feature_num][dim];
	protected static HashMap<Integer,String> clust_labels=new HashMap<Integer,String>();
	protected static List<HashSet<String>> clusters=new ArrayList<HashSet<String>>();
	protected static HashMap<Integer,HashSet<String>> cid_feature=new HashMap<Integer,HashSet<String>>();
	
	protected static DefaultMutableTreeNode ROOT=new DefaultMutableTreeNode("phone");
	public static void main(String[] args){
//		//String method="direct";//"graph";"agglo";"rbr";"bagglo";"br";
//		//Integer clust_num=3;
//		/*if(args.length>0)
//			clust_num=Integer.parseInt(args[0]);
//		if(args.length>1)
//			method=args[1];*/
//		
//		String[] features={"look", "screen", "price", "camera", "time", "speed", "battery", "version", "quality", "performance", "storage", "user", "feature", "purchase", "design", "value", "part", "size"};
//		//String path="E:/Tsinghua/±œ…Ë/project1/remote_service/";
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
		if(args.length>0)
			th=Double.parseDouble(args[0]);
		if(args.length>1)
			dim=Integer.parseInt(args[1]);
		bisection();
        JFrame f = new JFrame("JTreeDemo");
        JTree T=new JTree(ROOT);
        //f.add(T);
        JScrollPane scrollPane_2 = new JScrollPane();
        f.getContentPane().add(scrollPane_2, BorderLayout.CENTER);
        f.setSize(900, 900);
        scrollPane_2.setViewportView(T);
        f.setVisible(true);

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
	public static void bisection(){
		try{
			FileReader fr=new FileReader(folder+"/context_vecs.txt");
			BufferedReader br=new BufferedReader(fr);
			br.readLine();
			String s;
			int n=0;
			while((s=br.readLine())!=null){
				String[] ss=s.split(" ");
				for(int m=0;m<dim;m++)
					context[n][m]=Integer.parseInt(ss[m]);
				n++;
			}
			br.close();
			fr.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		read_features();
		bis_split(all_features,ROOT);
		get_hypernym();
		String result="";
		Iterator<Entry<Integer,HashSet<String>>> iter=cid_feature.entrySet().iterator();
		while(iter.hasNext()){
			Entry<Integer,HashSet<String>> ent=iter.next();
			result=result+"cid = "+ent.getKey().toString()+" hypernym = "+clust_labels.get(ent.getKey())+"\n";
			Iterator<String> it=ent.getValue().iterator();
			while(it.hasNext())
				result=result+it.next()+' ';
			result+='\n';
		}
		try{
			FileWriter fw=new FileWriter(folder+"/result_direct_cos_bisc.txt");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(result);
			bw.close();
			fw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void bis_split(List<String> features,DefaultMutableTreeNode root){
		if(compute_ISim(features)>=th||features.size()<2){
			for(String f:features)
				root.add(new DefaultMutableTreeNode(f));
			return;
		}
		write_input_matrix(features);
		String command=folder+"/vcluster.exe -clmethod=direct -sim=cos -rowmodel=maxtf "+folder+"/Input_Matrix_"+cid.toString()+".txt 2";
		CommandUtil util = new CommandUtil();
        util.executeCommand(command);
        printList(util.getStdoutList());
        System.out.println("--------------------");
        printList(util.getErroroutList());
        String outfile=folder+"/Input_Matrix_"+cid.toString()+".txt.clustering.2";
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
        DefaultMutableTreeNode node1=new DefaultMutableTreeNode(hypernym(cid_feature.get(cid)));
        DefaultMutableTreeNode node2=new DefaultMutableTreeNode(hypernym(cid_feature.get(cid+1)));
        root.add(node1);
        root.add(node2);
        List<String> features1=new ArrayList<String>();//[cid_feature.get(cid).size()];
        List<String> features2=new ArrayList<String>();//String[cid_feature.get(cid+1).size()];
        Iterator<String> it=cid_feature.get(cid).iterator();
        while(it.hasNext()){
        	features1.add(it.next());
        }
        it=cid_feature.get(cid+1).iterator();
        while(it.hasNext()){
        	features2.add(it.next());
        }
        cid+=2;
        bis_split(features1,node1);
        bis_split(features2,node2);
	}
	public static int get_id(String f){
		for(int m=0;m<all_features.size();m++){
			if(all_features.get(m).equals(f))
				return m;
		}
		return -1;
	}
	public static void write_input_matrix(List<String> features){
		try{
			FileWriter fw=new FileWriter(folder+"/Input_Matrix_"+cid.toString()+".txt");
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
			clust_labels.put(ent.getKey(), hypernym(ent.getValue()));
		}
	}
	public static String hypernym(HashSet<String> Features){
		double[] overlap_vec=new double[dim];
		for(String f:Features){
			int id=get_id(f);
			for(int k=0;k<dim;k++){
				if(context[id][k]>0)
					overlap_vec[k]+=1;
			}
		}
		for(int k=0;k<dim;k++){
			if(overlap_vec[k]<(Features.size()*cover))//||clust_labels.containsValue(all_features.get(k)))
				overlap_vec[k]=0;
			else
				overlap_vec[k]=1;
		}
		for(String f:Features){
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
		/*s=s+"="+String.valueOf(max_value-1)+" ";
		for(int k=1;k<dim;k++){
			if(overlap_vec[k]>0){
				s=s+all_features.get(k-1)+"="+String.valueOf(overlap_vec[k]-1)+" ";
			}
		}*/
		
		return s;
	}
	
	
 	public static void read_features(){
		try{
			FileReader fr=new FileReader(folder+"/Row_labels.txt");
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
	}
    public static void printList(List<String> list){
        for (String string : list) {
            System.out.println(string);
        }
    }
}
