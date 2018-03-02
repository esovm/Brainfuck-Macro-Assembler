/**
 * 
 */
package dvx.lang.brainfuck.assembler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import dvx.lang.brainfuck.runtime.Engine;

/**
 * @author devauem
 *
 */
public class Assembler {
	
	class StackBookmark {
		
		private List<Map<String,Integer>> sbm;
		
		public StackBookmark() {
			sbm = new ArrayList<Map<String,Integer>>();
			sbm.add(new HashMap<String,Integer>());
		}
		
		public boolean addBookmark(String name, int offset) {
			if (existBookmark(name)) {
				return false;
			}
			sbm.get(sbm.size()-1).put(name,offset);
			return true;
		}
		
		/**
		 * Get Stack Bookmark offset for a particular name
		 * @param name name of the bookmark
		 * @return  offset  or -1 if name is unknown
		 */
		public int getBookmark(String name) {
			for (int i=sbm.size()-1; i >=0 ; i--) {
				if (sbm.get(i).containsKey(name)) {
					return sbm.get(i).get(name);
				}
			}
			return -1;
		}
		
		public StackBookmark nextLevel() {
			sbm.add(new HashMap<String,Integer>());
			return this;
		}
		
		public StackBookmark previousLevel() {
			if (sbm.size() == 1 ) return null;
			sbm.remove(sbm.size()-1);
			return this;
		}
		
		private boolean existBookmark(String name) {
			for (int i=sbm.size()-1; i >=0 ; i--) {
				if (sbm.get(i).containsKey(name)) {
					return true;
				}
			}
			return false;
		}
	}
	
	class Variable {
		private String varName;
		private int useCount;
		private int idx;
		private int offset;
		private int size;
		private Operation opDeclare; // operation that declare the variable
		
		
		
		Variable(String name, Operation opDeclare, int idx, int size) {
			init (name,opDeclare, idx,size);
		}
		
		Variable(String name, Operation opDeclare, int idx) {
			init (name,opDeclare, idx,1);
		}
		
		private void init (String name, Operation opDeclare, int idx, int size) {
			this.varName = name;
			this.useCount = 0;
			this.opDeclare = opDeclare;
			this.idx = idx;
			this.size = size;
			this.offset=size; // wrong offset by default
		}
		
		public String getName() {
			return varName;
		}
		
		public Variable addUse() {
			useCount++;
			return this;
		}
		
		public int getUse() {
			return useCount;
		}
		
		public boolean isUsed() {
			return useCount != 0;
		}
		
		public int getIndex() {
			return idx;
		}
		
		public int getSize() {
			return size;
		}
		
		public Operation getOperation() {
			return opDeclare;
		}
		
		public int getOffset() {
			return offset;
		}
		
		public Variable setOffset(int offset) {
			this.offset = offset;
			return this;
		}
		
	}

	class Operation {
		public String rawInstr;
		public int lineNum;
		public int colNum;
		public String comment;
		public AsmToken asmToken;
		public AsmOpeType asmOpeType;
		public String asmOpeValue;
		public String errorMsg;
		public List<Operation> opeList;
		public String BF;
		public String BF_post;
		public boolean okForAt;
		
		Operation(Assembler asm, String rawInstr, int lineNum, int colNum) {
			this.BF = "";
			this.BF_post = "";
			this.opeList = new ArrayList<Operation>();
			this.rawInstr = rawInstr;
			this.lineNum = lineNum;
			this.colNum = colNum;
			this.asmToken = AsmToken.UNKNOWN;
			this.comment = "";
			this.errorMsg = "";
			this.asmOpeType = AsmOpeType.NONE;
			this.asmOpeValue = "";
			this.okForAt = false;
			rawInstr = rawInstr.replace('\t', ' ');
			rawInstr = rawInstr.trim();
			if (rawInstr.length()>0) {
				int idxComment = rawInstr.indexOf('#');
				if (idxComment > 0) {
					this.comment = rawInstr.substring(idxComment+1);
					rawInstr = rawInstr.substring(0, idxComment).trim();
				}
			}
			if (rawInstr.length() == 0) {			// empty line
				asmToken = AsmToken.EMPTY_LINE;
			} else if (rawInstr.charAt(0) == '#') {	// comment line
				asmToken = AsmToken.COMMENT;
				comment = rawInstr.substring(1);
				asmOpeValue = this.comment;
			} else {
				// singularize nb spaces
				rawInstr = rawInstr.replaceAll("[ ]+", " ");
				// get operator and operand
				int idxSep = rawInstr.indexOf(' ');
				String operand = "";
				boolean hasOperand = false;
				String instr = rawInstr.toUpperCase();;
				if (idxSep>0) {
					operand = rawInstr.substring(idxSep+1).trim();
					hasOperand = true;
					asmOpeValue = operand;
					instr = rawInstr.substring(0,idxSep).toUpperCase();
				}
				try {
					// match instruction and operand
					if (instr.equals("RESET")) { // reset
						asmToken = AsmToken.RESET;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						okForAt = true;
					} else if (instr.equals("IN")) { // get Input
						asmToken = AsmToken.IN;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						okForAt = true;
					} else if (instr.equals("OUT")) { // set output
						asmToken = AsmToken.OUT;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						okForAt = true;
					} else if (instr.equals("INC")) { // increment (++)
						asmToken = AsmToken.INC;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						okForAt = true;
					} else if (instr.equals("DEC")) { // decrement (--)
						asmToken = AsmToken.DEC;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						okForAt = true;
					} else if (instr.equals("SET")) { // set immediate
						asmToken = AsmToken.SET;
						asmOpeType = AsmOpeType.IMMEDIATE;
						if (!hasOperand) {
							throw new Exception(instr+" without operand");
						}
						if (operand.matches("^'.'$")) {
							asmOpeValue=String.valueOf((byte)operand.charAt(1));
							comment = operand + " " + comment;
						} else 	if (!operand.matches("^25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?$")) {
							throw new Exception("SET need a immediate integer");
						}
						okForAt = true;
					} else if (instr.equals("ADD")) {
						asmToken = AsmToken.ADD;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							asmOpeType = AsmOpeType.IMMEDIATE;
							if (operand.matches("^'.'$")) {
								asmOpeValue=String.valueOf((byte)operand.charAt(1));
								comment = operand + " " + comment;
							} else 	if (!operand.matches("^25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?$")) {
								throw new Exception(instr+" operand must be an integer ");
							}
							okForAt = true;
						}
					} else if (instr.equals("SUB")) {
						asmToken = AsmToken.SUB;
						if (hasOperand) {
							asmOpeType = AsmOpeType.IMMEDIATE;
							if (operand.matches("^'.'$")) {
								asmOpeValue=String.valueOf((byte)operand.charAt(1));
								comment = operand + " " + comment;
							} else 	if (!operand.matches("^25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?$")) {
								throw new Exception(instr+" operand must be an integer ");
							}
							okForAt = true;
						}
					} else if (instr.equals("MUL")) {
						asmToken = AsmToken.MUL;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("DIV")) {
						asmToken = AsmToken.DIV;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("BOOL")) {
						asmToken = AsmToken.BOOL;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("NOT")) {
						asmToken = AsmToken.NOT;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("OR")) {
						asmToken = AsmToken.OR;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("AND")) {
						asmToken = AsmToken.AND;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("EQUAL")) {
						asmToken = AsmToken.EQUAL;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("DIFF")) {
						asmToken = AsmToken.DIFF;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("SUP")) {
						asmToken = AsmToken.SUP;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("INF")) {
						asmToken = AsmToken.INF;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("DUP")) {
						asmToken = AsmToken.DUP;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("DROP")) {
						asmToken = AsmToken.DROP;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("SWAP")) {
						asmToken = AsmToken.SWAP;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("AT")) {
						asmToken = AsmToken.AT;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						if (operand.charAt(0)=='-') {
							asmOpeValue=operand.substring(1);
							if (asmOpeValue.matches("^[0-9]+$")) {
								asmOpeType = AsmOpeType.FROMSTACK;
							} else {
								asmOpeType = AsmOpeType.FROMSTACKBM;
							}	
						} else {
							asmOpeType = AsmOpeType.VARNAME;
							if (!operand.matches("^[_A-Za-z][_A-Za-z0-9.]*$") || 
									 operand.toLowerCase().equals("out")) {
								throw new Exception(instr+" with incorrect operand syntax");
							}
							Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)\\.(pos|POS)$");
							Matcher m = p.matcher(asmOpeValue);
							if (m.matches()) { // size or pos
								String varName = m.group(1);
								boolean b = asm.useVar(varName);
								if (!b) { // not declared variable
									throw new Exception("Unknown variable '"+varName+"'");
								}
							} else { // not pos
								boolean b = asm.useVar(operand);
								if (!b) { // not declared variable
									if (asm.sbm.getBookmark(operand) == -1) {
										throw new Exception("Unknown variable/stack bookmark '"+operand+"'");
									} else {
										asmOpeType =  AsmOpeType.FROMSTACKBM;
									}
								}
							}
						}
					} else if (instr.equals("PUSH")) {
						asmToken = AsmToken.PUSH;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						if (operand.charAt(0)=='-') {
							asmOpeValue=operand.substring(1);
							if (asmOpeValue.matches("^[0-9]+$")) {
								asmOpeType = AsmOpeType.FROMSTACK;
							} else {
								asmOpeType = AsmOpeType.FROMSTACKBM;
							}
						} else if (operand.matches("^25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?$")) {
							asmOpeType = AsmOpeType.IMMEDIATE;
						} else if (operand.toLowerCase().equals("in")) {
							asmOpeType = AsmOpeType.IN;
						} else if (operand.matches("^'.'$")) {
							asmOpeType = AsmOpeType.IMMEDIATE;
							asmOpeValue=String.valueOf((byte)operand.charAt(1));
							comment = operand + " " + comment;
						} else {
							asmOpeType = AsmOpeType.VARNAME;
							if (!operand.matches("^[_A-Za-z][_A-Za-z0-9.]*$") || 
									 operand.toLowerCase().equals("out")) {
								throw new Exception(instr+" with incorrect operand syntax");
							}
							Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)\\.(size|SIZE|pos|POS)$");
							Matcher m = p.matcher(asmOpeValue);
							if (m.matches()) { // size or pos
								String varName = m.group(1);
								String sizePos = m.group(2);
								boolean b = asm.useVar(varName);
								if (!b) { // not declared variable
									throw new Exception("Unknown variable '"+varName+"'");
								}
								sizePos = sizePos.toUpperCase();
								if (sizePos.equals("SIZE")) { // transform into immediate
									asmOpeType = AsmOpeType.IMMEDIATE;
									asmOpeValue = String.valueOf(asm.getVar(varName).getSize());
									comment = "Size of variable '"+ varName +"'. " + comment;
								}
							} else { // not size nor pos
								boolean b = asm.useVar(operand);
								if (!b) { // not declared variable
									if (asm.sbm.getBookmark(operand) == -1) {
										throw new Exception("Unknown variable/stack bookmark '"+operand+"'");
									} else {
										asmOpeType =  AsmOpeType.FROMSTACKBM;
									}
								}
							}
						}
					} else if (instr.equals("POP")) {
						asmToken = AsmToken.POP;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand" );
						}
						if (operand.charAt(0)=='-') {
							asmOpeValue=operand.substring(1);
							if (asmOpeValue.matches("^[0-9]+$")) {
								asmOpeType = AsmOpeType.FROMSTACK;
							} else {
								asmOpeType = AsmOpeType.FROMSTACKBM;
							}
						} else if (operand.toLowerCase().equals("out")) {
							asmOpeType = AsmOpeType.OUT;
						} else  {
							asmOpeType = AsmOpeType.VARNAME;
							if (!operand.matches("^[_A-Za-z][_A-Za-z0-9.]*$") || 
								 operand.toLowerCase().equals("in")) {
								throw new Exception(instr+" with incorrect operand syntax");
							}
							Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)\\.(size|SIZE|pos|POS)$");
							Matcher m = p.matcher(asmOpeValue);
							if (m.matches()) { // size or pos
								String varName = m.group(1);
								String sizePos = m.group(2);
								boolean b = asm.useVar(varName);
								if (!b) { // not declared variable
									throw new Exception("Unknown variable '"+varName+"'");
								}
								sizePos = sizePos.toUpperCase();
							} else { // not size nor pos
								boolean b = asm.useVar(operand);
								if (!b) { // not declared variable
									if (asm.sbm.getBookmark(operand) == -1) {
										throw new Exception("Unknown variable/stack bookmark '"+operand+"'");
									} else {
										asmOpeType =  AsmOpeType.FROMSTACKBM;
									}
								}
							}

						}
					} else if (instr.equals("BF")) {
						asmToken = AsmToken.BF;
						asmOpeType = AsmOpeType.BF;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
					} else if (instr.equals("AROTR")) {
						asmToken = AsmToken.AROTR;
						asmOpeType = AsmOpeType.VARNAME;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						boolean b = asm.useVar(asmOpeValue);
						if (!b) {
							throw new Exception("Unknonw variable '"+ asmOpeValue+"'");
						}
					} else if (instr.equals("AROTL")) {
						asmToken = AsmToken.AROTL;
						asmOpeType = AsmOpeType.VARNAME;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						boolean b = asm.useVar(asmOpeValue);
						if (!b) {
							throw new Exception("Unknonw variable '"+ asmOpeValue+"'");
						}
					} else if (instr.equals("ARESET")) {
						asmToken = AsmToken.ARESET;
						asmOpeType = AsmOpeType.VARNAME;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						boolean b = asm.useVar(asmOpeValue);
						if (!b) {
							throw new Exception("Unknonw variable '"+ asmOpeValue+"'");
						}
					} else if (instr.equals("ASET")) {
						asmToken = AsmToken.ASET;
						asmOpeType = AsmOpeType.VARNAME;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						// analyse the var name
						Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)\\s*.$");
						Matcher m = p.matcher(asmOpeValue);
						if (m.matches()){
							boolean b = asm.useVar(m.group(1));
							if (!b) {
								throw new Exception("Unknonw variable '"+ m.group(1)+"'");
							}
						} else {
							throw new Exception("Bad " + instr + " operand");
						}
					} else if (instr.equals("VAR")) {
						asmToken = AsmToken.VARIABLE;
						asmOpeType = AsmOpeType.VARNAME;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						if (asmOpeValue.equalsIgnoreCase("in") ||
							asmOpeValue.equalsIgnoreCase("out")) {
							throw new Exception("variable name 'IN' and 'OUT' are reserved");
						}
						boolean b = asm.declareVar(asmOpeValue,  this); // declare the variable
						if (!b) {
							throw new Exception(this.errorMsg);
						}
					} else if (instr.equals("SBM")) {
						asmToken = AsmToken.SBM;
						asmOpeType = AsmOpeType.BMNAME;
						if (!hasOperand) {
							throw new Exception(instr+" with no operand");
						}
						asm.sbm.addBookmark(asmOpeValue, 0);
					} else if (instr.equals("IF")) {
						asmToken = AsmToken.IF;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						asm.sbm.nextLevel();
					} else if (instr.equals("ELSE")) {
						asmToken = AsmToken.ELSE;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						asm.sbm.previousLevel();
						asm.sbm.nextLevel();
					} else if (instr.equals("LOOP")) {
						asmToken = AsmToken.LOOP;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						asm.sbm.nextLevel();
					} else if (instr.equals("WHILE")) {
						asmToken = AsmToken.WHILE;
						asmOpeType = AsmOpeType.NONE;
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						asm.sbm.nextLevel();
					} else if (instr.equals("ENDAT") || instr.equals("TA")) {
						asmToken = AsmToken.ENDSCOPE;
						asmOpeType = AsmOpeType.AT; // to be determined
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
					} else if (instr.equals("ENDIF") || instr.equals("FI")) {
						asmToken = AsmToken.ENDSCOPE;
						asmOpeType = AsmOpeType.IF; // to be determined
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						asm.sbm.previousLevel();
					} else if (instr.equals("ENDLOOP")) {
						asmToken = AsmToken.ENDSCOPE;
						asmOpeType = AsmOpeType.LOOP; // to be determined
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						asm.sbm.previousLevel();
					} else if (instr.equals("ENDWHILE") || instr.equals("WEND")) {
						asmToken = AsmToken.ENDSCOPE;
						asmOpeType = AsmOpeType.WHILE; // to be determined
						if (hasOperand) {
							throw new Exception(instr+" can't accept operand");
						}
						asm.sbm.previousLevel();
					} else {
						throw new Exception ("Unknown instruction '" + instr + "'");
					}
				} catch (Exception e) {
					errorMsg += e.getMessage() + "; ";
				}
			}
			
		}
		
		
		public int size() {
			return this.opeList.size();
		}

		public int sizeDeep() {
			int currSize = size();
			for (Operation op : opeList) {
				currSize += op.sizeDeep();
			}
			return currSize;
		}
	}

	private List<Operation> lOpe;
	private List<Operation> lOpeTree;
	private Map<String, Variable> mVariables;
	private List<String> errorList;
	private StackBookmark sbm;
	private String preCompiled;
	private String preCompiledjs;
	private String include;
	
	public Assembler(InputStream isAsmSource, String include)  {
		sbm = new StackBookmark();
		this.include = include;
		lOpe = new ArrayList<Operation>();
		lOpeTree = new ArrayList<Operation>();
		String strAsmSource="<no precompiled source>";
		preCompiledjs = "<no js for precompiled source>";
		// construct error list
		errorList = new ArrayList<String>();
		try {
			strAsmSource = preCompile(isAsmSource);
		} catch (IOException | ScriptException e) {
			errorList.add(e.getMessage());
		}
		preCompiled = strAsmSource;
		if (hasError()) return;
	
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(strAsmSource.getBytes())));
		String line = null;
		int lineCount = 0;
		mVariables = new HashMap<String, Variable>(); // new variable map
		
		// get Operations // syntax check +  var declaration + var useage
		try {
			while((line = in.readLine()) != null) {
				lineCount++;
				Operation op = new Operation(this,line,lineCount,1);
				lOpe.add(op);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// check not used variable
		for (Variable var: mVariables.values()) {
			if (!var.isUsed()) {
				var.getOperation().errorMsg += "Unused variable; ";
			}
		}
		// structure the program
		lOpeTree = toTree(reduce(lOpe),0,1);
		
		// construct error list
		errorList = new ArrayList<String>(); 
		for (Operation op: lOpe) {
			if (op.errorMsg.length() > 0) {
				errorList.add("L#"+ String.format("%-4d", op.lineNum) +": " + op.rawInstr + "\n      " + op.errorMsg.replace(";","\n      "));
			}
		}
		if (hasError()) return; // stop here if there is/are error(s)
		sbm = new StackBookmark(); // raz previous sbm instance (used during source parsing)
		// here there is no error then produce the BF code
		compile(lOpeTree,0,1,false);
		// re-construct error list
		errorList = new ArrayList<String>(); 
		for (Operation op: lOpe) {
			if (op.errorMsg.length() > 0) {
				errorList.add("L#"+ String.format("%-4d", op.lineNum) +": " + op.rawInstr + "\n      " + op.errorMsg.replace(";","\n      "));
			}
		}

		
	}
	
	private String preCompile(InputStream isAsmSource) throws IOException, ScriptException {
		String result="";
		BufferedReader in = new BufferedReader(new InputStreamReader(isAsmSource));
		String script = genMacro(in,"_main","",false)+"_main();";
		// DEBUG
		//System.out.println("JScode:\n"+script);

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		preCompiledjs = script;
		result = String.valueOf(engine.eval(script));
		return result;
	}

	private String genMacro( BufferedReader in,
							 String macroName,
							 String macroParams,
							 boolean isInclude) throws IOException {
		StringBuffer result = new StringBuffer();
		if (!isInclude) {
			result.append("var " + macroName +" = function("+macroParams+"){\n");
			result.append("var _str"+macroName +";\n");
			result.append("_str"+macroName +"='';\n");
		}
		String line;
		Pattern p_beginmacro = Pattern.compile("^\\s*(macro|MACRO)\\s+([_A-Za-z][_A-Za-z0-9]*)\\s*(\\(([^)]*)\\)).*$");
		Pattern p_endmacro = Pattern.compile("^\\s*(endmacro|ENDMACRO)\\s*$");
		Pattern p_instr = Pattern.compile("^\\s*([_A-Za-z][_A-Za-z0-9]*)\\s*(\\([^)]*\\)).*$");
		Pattern p_define = Pattern.compile("^\\s*(define|DEFINE)\\s+([_A-Za-z][_A-Za-z0-9]*)\\s+(\\S.*)$");
		Pattern p_include = Pattern.compile("^\\s*(include|INCLUDE)\\s+(\\S+)\\s*$");
		Pattern p_js = Pattern.compile("^\\s*js\\s*(.+)$");
		while ((line = in.readLine()) != null) {
			Matcher m_beginmacro= p_beginmacro.matcher(line);
			Matcher m_endmacro = p_endmacro.matcher(line);
			Matcher m_instr = p_instr.matcher(line);
			Matcher m_define = p_define.matcher(line);
			Matcher m_include = p_include.matcher(line);
			Matcher m_js = p_js.matcher(line);
			if (m_beginmacro.matches()) {
				result.append(
						genMacro(in,
								 m_beginmacro.group(2),
								 m_beginmacro.group(4),
								 false
								 )
						);
			} else if (m_endmacro.matches()) {
				if (!isInclude) {
					result.append("return _str"+macroName+";\n");
					result.append("}\n");
				}
				return result.toString();
			} else if (m_instr.matches()) {
				result.append("_str"+macroName+"+=" +
						toJSstring("#(begin macro)" + line)+" + '\\n';\n");

				result.append("_str"+macroName+"+=" +
							m_instr.group(1).replaceAll("(\\{([^}]+)\\})", "$2") + 
							m_instr.group(2).replaceAll("(\\{([^}]+)\\})", "$2") +
							";\n");
				result.append("_str"+macroName+"+=" +
						toJSstring("#(end macro)" + line)+" + '\\n';\n");

			} else if (m_js.matches()) {
				result.append("_str"+macroName+"+=" +
						toJSstring("#(js) " + m_js.group(1))+" + '\\n';\n");
				result.append(m_js.group(1) + "\n");
			} else if (m_define.matches()) {
				result.append("var " + m_define.group(2) + ";" +
						m_define.group(2) + "=" +
						toJSstring(m_define.group(3))+";\n");
			} else if (m_include.matches()) {
				result.append("_str"+macroName+"+=" +
						toJSstring("#(begin include)" + m_include.group(2))+" + '\\n';\n");
				BufferedReader in_include = new BufferedReader(
												new InputStreamReader(
													new FileInputStream(
														include + m_include.group(2)
														)
													)
												);
				result.append( genMacro(in_include, macroName,"",true));
				result.append("_str"+macroName+"+=" +
						toJSstring("#(end include)" + m_include.group(2))+" + '\\n';\n");
			} else {
				result.append("_str"+macroName+"+=" +
							toJSstring(line)+" + '\\n';\n");
			}
		}
		if (!isInclude) {
			result.append("return _str"+macroName+";\n");
			result.append("}\n");
		}
		return result.toString();
	}
	
	private String toJSstring(String oriString) {
		oriString = oriString.replace("\\", "\\\\")
				 .replace("'", "\\'");
		oriString = oriString.replaceAll("(\\{([^}]+)\\})", "' + $2 + '");
		return "'"+oriString+"'";
	}
	
	
	public String getPreCompiled() {
		return this.preCompiled;
	}

	public String getPreCompiledJS() {
		return this.preCompiledjs;
	}
	
	public String getBFOnly() {
		return getBFOnly(false, 0);
	}

	public String getBFOnly(boolean optimize) {
		return getBFOnly(optimize, 0);
	}
	
	public String getBFOnly(int lineSize) {
		return getBFOnly(false, lineSize);
	}
	
	public String getBFOnly(boolean optimize,int lineSize) {
		OutputStream output = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(output);
		getBFCode(ps);
		ps.flush();
		String strBFCode = output.toString();
		strBFCode = keepBFOnly(strBFCode);
		if (optimize) {
			strBFCode = optimizeBF(strBFCode);
		}
		return lineBreaker(strBFCode,lineSize);
	}
	
	private String keepBFOnly(String bfcode) {
		if (bfcode == null || bfcode.length() == 0) return bfcode;
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < bfcode.length(); i++) {
			char c = bfcode.charAt(i);
			switch (c) {
				case '+':
				case '-':
				case '>':
				case '<':
				case '[':
				case ']':
				case ',':
				case '.':
					result.append(c);
					break;
				default:
					break;
			}
		}
		return result.toString();
	}
	
	private String optimizeBF(String bfcode) {
		if (bfcode == null) return null;
		if (bfcode.length()<2) return bfcode;
		int lengthbefore;
		do {
			lengthbefore = bfcode.length();
			bfcode.replace("+-", "")
				  .replace("-+", "")
				  .replace("<>", "")
				  .replace("><", "");
		} while (lengthbefore> bfcode.length());
		return bfcode;
	}
	
	
	public Assembler getBFCode(PrintStream ps) {
		printCode(lOpeTree,ps);
		return this;
	}
	
	private void printCode(List<Operation> ltree, PrintStream ps) {
		String preComment = "";
		for (Operation ope: ltree) {
			preComment = "";
			if (ope.comment.length()>0) preComment = " //";
			if (ope.BF.length() >0 ) {
				if (ope.asmToken != AsmToken.VARIABLE){
					String prolog = "L#"+ String.format("%-4d", ope.lineNum) + ": ";
					String token = String.format("%-9s",ope.asmToken) ;
					String operand = (ope.asmOpeType==AsmOpeType.FROMSTACK || ope.asmOpeType==AsmOpeType.FROMSTACKBM?"STACK(":"")+
							String.format("%-32s",ope.asmOpeValue + (ope.asmOpeType==AsmOpeType.FROMSTACK || ope.asmOpeType==AsmOpeType.FROMSTACKBM?")":""));
					String comment = preComment + BFTransparent(ope.comment);
					if (ope.asmToken == AsmToken.BF) {
						operand = "/bf instructions/";
					}
					if (ope.asmToken == AsmToken.ENDSCOPE) {
						if (ope.asmOpeType == AsmOpeType.IF) {
							token = String.format("%-9s", "ENDIF");
						} else if (ope.asmOpeType == AsmOpeType.LOOP) {
							token = String.format("%-9s","ENDLOOP");
						} else if (ope.asmOpeType == AsmOpeType.WHILE) {
							token = String.format("%-9s","WEND");
						}
					}
					operand = operand.replace('.', '~');
					ps.println(prolog + token + operand + comment);
				}
				ps.println(lineBreaker(ope.BF,32));
			}
			printCode(ope.opeList,ps);
			if (ope.BF_post.length() >0) ps.println(lineBreaker(ope.BF_post,32));
		}
	}
	
	private String BFTransparent(String text) {
		if (text == null) return null;
		if (text.length()==0) return text;
		return text.replace(".", "/point/").
					replace(",", "/comma/").
					replace("+", "/plus/").
					replace("-", "/minus/").
					replace(">", "/superior/").
					replace("<", "/inferior/").
					replace("[", "/open bracket/").
					replace("]", "/close bracket/");
	}
	
	private String lineBreaker(String aLine, int maxLineSize) {
		if (aLine == null) return null;
		if (maxLineSize <1) return aLine;
		
		StringBuffer result=new StringBuffer();
		String current=aLine;
		while (current.length()>maxLineSize) {
			result.append(current.substring(0, maxLineSize)).
			append("\n");
			current = current.substring(maxLineSize);
		}
		if (current.length()>0) result.append(current);
		return result.toString();
	}
	
	private String repeat(String str, int nb) {
		String result="";
		while (nb>0) {
			nb--;
			result+=str;
		}
		return result;
	}
	
	private int getOffset(String bf) {
		int result = 0;
		for (int i = 0; i < bf.length() ; i++) {
			char c = bf.charAt(i);
			if (c == '>') result++;
			if (c == '<') result--;
		}
		return result;
	}
	
	
	private List<Operation> reduce(List<Operation> oriList) {
		List<Operation> result = new ArrayList<Operation>();
		for (Operation ope: oriList) {
			if (ope.asmToken != AsmToken.EMPTY_LINE &&
				ope.asmToken != AsmToken.COMMENT &&
				ope.asmToken != AsmToken.VARIABLE && 
				ope.asmToken != AsmToken.UNKNOWN) {
				result.add(ope);
			}
		}
		return result;
	}
	
	private void compile(List<Operation> ltree,int offset, int level, boolean inAt) {
		int nb=0;
		
		String BF_BOOL = ">[-]<[>+<[-]]>[<+>-]<";
		String BF_NOT = optimizeBF(BF_BOOL + "-" + BF_BOOL);
		String BF_ADD = "[<+>-]<";
		String BF_SUB = "[<->-]<";
		String BF_EQUAL = optimizeBF(BF_SUB + BF_BOOL + BF_NOT);
		String BF_DIFF = optimizeBF(BF_SUB + BF_BOOL);
		String BF_MUL = ">>[-]<<<[->[>+>+<<-]>[<+>-]>[<+>-]<[>+<-]<<]>[-]>>[<<<+>>>-]<<<";
		String BF_DROP = "[-]<";
		String BF_DUP = "[>+>+<<-]>>[<<+>>-]<";
		int offsetBefore = offset;
		for (int i=0; i< ltree.size(); i++) {
			Operation ope = ltree.get(i);
			if (inAt && !ope.okForAt) {
				ope.errorMsg += "Can't be used in AT block; ";
				continue;
			}
			switch (ope.asmToken) {
				case COMMENT:
					ope.BF="";
					break;
				case EMPTY_LINE:
					ope.BF="";
					break;
				case UNKNOWN:
					ope.BF = "";
					break;
				case VARIABLE:
					ope.BF = "";
					break;
				case RESET:
					ope.BF = "[-]";
					break;
				case IN:
					ope.BF = ",";
					break;
				case OUT:
					ope.BF = ".";
					break;
				case INC:
					ope.BF = "+";
					break;
				case DEC:
					ope.BF = "-";
					break;
				case SET:
					ope.BF = "[-]"+ repeat("+",Integer.parseInt(ope.asmOpeValue));
					break;
				case ADD:
					switch (ope.asmOpeType) {
						case IMMEDIATE:
							ope.BF = repeat("+",Integer.parseInt(ope.asmOpeValue));
							break;
						case NONE: 
							ope.BF = BF_ADD;
							offset--;
							break;
						default:
							ope.BF = "[Error]";
					}
					break;
				case SUB:
					switch (ope.asmOpeType) {
						case IMMEDIATE:
							ope.BF = repeat("-",Integer.parseInt(ope.asmOpeValue));
							break;
						case NONE: 
							ope.BF = BF_SUB;
							offset--;
							break;
						default:
							ope.BF = "[Error]";
					}
					break;
				case MUL:
					offset--;
					ope.BF = BF_MUL;
					break;
				case DIV:
					ope.BF = "[>+>+<<-]>[<+>-] >[<+>-]<<[-]<[->+>-[>+>>]>[+[-<+>]>+>>]<<<<<<]>[-]>[-]>[<<<+>>>-]>[<<<+>>>-]<<<";
					break;
				case BOOL:
					ope.BF = BF_BOOL;
					break;
				case NOT:
					ope.BF = BF_NOT;
					break;
				case OR:
					ope.BF = "<" + BF_BOOL + ">" + BF_BOOL  + BF_ADD + BF_BOOL;
					offset--;
					break;
				case AND:
					ope.BF = "<" + BF_BOOL + ">" + BF_BOOL + BF_MUL;
					offset--;
					break;
				case EQUAL:
					ope.BF = BF_EQUAL;
					offset--;
					break;
				case DIFF:
					ope.BF = BF_DIFF;
					offset--;
					break;
				case SUP:
					ope.BF = ">[-]>[-]+>[-]>[-]<<<<[>>>>+>+<<<<<-]>>>>[<<<<+>>>>-]>[<+>-]<"+
							 ">[-]<<<<<<[>>>>>>+>+<<<<<<<-]>>>>>>[<<<<<<+>>>>>>-]>[<+>-]<"+
							 ">[-]<<<+>+<[->-[>]<<]<[-]<[-<]<[-]<[-]>>>>>[-<<<<<+>>>>>]<<<<<"+
							 ">[-]<[>+<[-]]>[<+>-]<";
					offset--;
					break;
				case INF:
					ope.BF = ">[-]>[-]+>[-]>[-]<<<<[>>>>+>+<<<<<-]>>>>[<<<<+>>>>-]>[<+>-]<"+
							 ">[-]<<<<<<[>>>>>>+>+<<<<<<<-]>>>>>>[<<<<<<+>>>>>>-]>[<+>-]<"+
							 ">[-]<<<+>+<[->-[>]<<]<[-]<[-<]<[-]<[-]>>>>>>[-<<<<<<+>>>>>>]<<<<<<"+
							 ">[-]<[>+<[-]]>[<+>-]<";
					offset--;
					break;
				case BF:
					ope.BF = ope.asmOpeValue;
					offset+= getOffset(ope.BF);
					break;
				case SBM:
					if (!sbm.addBookmark(ope.asmOpeValue,offset)) {
						ope.errorMsg += "Stack bookmark '"+ ope.asmOpeValue + "' already exist; ";
					} else {
						ope.BF = " ";
					}	
					break;
				case PUSH:
					switch (ope.asmOpeType) {
						case FROMSTACKBM:
							nb = sbm.getBookmark(ope.asmOpeValue);
							if (nb == -1) {
								ope.errorMsg += "Unknown stack bookmark name '" +  ope.asmOpeValue + "'; ";
							} else {
								nb = offset-nb;
								if (nb>=0) {
									ope.BF = ">[-]<" +
											repeat("<",nb) +
											 "[" +
											repeat(">",nb) +
											">+>+<<" +
											repeat("<",nb) + 
											"-]" +
											repeat(">",nb) +
											">[<" +
											repeat("<",nb) +
											"+" +
											repeat(">",nb) + 
											">-]>[<+>-]<";
								} else {
									ope.errorMsg += "Out of scope stack bookmark '" +  ope.asmOpeValue + "'; ";
								}
							}
							offset++;
							break;
						case FROMSTACK:
							nb = Integer.parseInt(ope.asmOpeValue);
							ope.BF = ">[-]<" +
									repeat("<",nb) +
									 "[" +
									repeat(">",nb) +
									">+>+<<" +
									repeat("<",nb) + 
									"-]" +
									repeat(">",nb) +
									">[<" +
									repeat("<",nb) +
									"+" +
									repeat(">",nb) + 
									">-]>[<+>-]<";
							offset++;
							break;
						case VARNAME:
							Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)\\.(pos|POS)$");
							Matcher m = p.matcher(ope.asmOpeValue);
							Variable v = getVar(ope.asmOpeValue);
							if (m.matches()) { // size or pos
								String varName = m.group(1);
								v = getVar(varName);
								nb = v.getOffset() - 2 + offset;
								if (v.getSize()==1) {
									ope.errorMsg += "Variable '" + varName +"' is not an array; ";
								}
								
							} else { // not size nor pos
								v = getVar(ope.asmOpeValue);
								nb = v.getOffset() + offset;
							}

							ope.BF = ">[-]<" +
									repeat("<",nb) +
									 "[" +
									repeat(">",nb) +
									">+>+<<" +
									repeat("<",nb) + 
									"-]" +
									repeat(">",nb) +
									">[<" +
									repeat("<",nb) +
									"+" +
									repeat(">",nb) + 
									">-]>[<+>-]<";
							offset++;							
							break;
						case IN:
							ope.BF = ">,";
							offset++;
							break;
						case IMMEDIATE:
							nb = Integer.parseInt(ope.asmOpeValue);
							ope.BF = ">[-]"+ repeat("+",nb);
							offset++;
							break;
						default:
							ope.BF="[Error]";
							break;
					}
					break;
				case POP:
					switch (ope.asmOpeType) {
						case FROMSTACKBM:
						nb = sbm.getBookmark(ope.asmOpeValue);
						if (nb == -1) {
							ope.errorMsg += "Unknown stack bookmark name '" +  ope.asmOpeValue + "'; ";
						} else {
							nb = offset-nb;
							if (nb>=0) {
								ope.BF =repeat("<",nb) +
										"[-]" +
										repeat(">",nb) +
										"[" +
										repeat("<",nb) + 
										"+" +
										repeat(">",nb) +
										"-]<";
							} else {
								ope.errorMsg += "Out of scope stack bookmark '" +  ope.asmOpeValue + "'; ";
							}
						}
						offset--;
						break;
						case FROMSTACK:
							nb = Integer.parseInt(ope.asmOpeValue);
							ope.BF =repeat("<",nb) +
									"[-]" +
									repeat(">",nb) +
									"[" +
									repeat("<",nb) + 
									"+" +
									repeat(">",nb) +
									"-]<";
							offset--;
							break;
						case VARNAME:
							Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)\\.(pos|POS)$");
							Matcher m = p.matcher(ope.asmOpeValue);
							Variable v = getVar(ope.asmOpeValue);
							if (m.matches()) { // is array pos
								String varName = m.group(1);
								v = getVar(varName);
								nb = v.getOffset() - 2 + offset;
								if (v.getSize()==1) {
									ope.errorMsg += "Variable '" + varName +"' is not an array; ";
								}
								
							} else { // is not array pos
								v = getVar(ope.asmOpeValue);
								nb = v.getOffset() + offset;
							}
							ope.BF =repeat("<",nb) +
									"[-]" +
									repeat(">",nb) +
									"[" +
									repeat("<",nb) + 
									"+" +
									repeat(">",nb) +
									"-]<";
							offset--;							
							break;
						case OUT:
							ope.BF = ".[-]<";
							offset--;
							break;
						default:
							ope.BF="[Error]";
							break;
					}
					break;
				case AROTR:
					do {
						Variable v = getVar(ope.asmOpeValue);
						nb = offset + v.getOffset();
						// move to pos :  = offset + variable offset -2
						ope.BF = repeat("<",nb - 2) +

								// decrement the pos of the variable
								//	 "-" +
							// if pos = 255 (-1) then pos = variable size-1
							/*DUp BOOL NOT */
								"[>+>+<<-]>[<+>-]>[<+>-][-]<[>+<[-]]>[<+>-][-]<[>+<[-]]>[<+>-]<->[-]<[>+<[-]]>[<+>-]<" +
									//BF_DUP +
								//	BF_BOOL + 
								//	BF_NOT +
								"[-<[-]"+ repeat("+",v.getSize() )  + ">]<-" + 

/*									
						// increment the pos of the variable
								 "+" +
						// if pos = variable size then pos = 0
								BF_DUP +
								">" + repeat("+",v.getSize()) + "<" +
								">[-<->]<" + 
								BF_BOOL + 
								BF_NOT +
								"[-<[-]>]<" + 
*/								
								
						
						
						// goto right temporary
								"<<" + // go to first byte of array
								repeat("<", v.getSize())+ // goto temporary right
						// clear the temporary
								"[-]" +
						// "variable size" time transfer left to here, move left
								repeat(">[-<+>]",v.getSize()) +
						// Transfer right temporary to offset
								repeat("<", v.getSize()) +
								"[-" +
								repeat(">", v.getSize()) +
								"+" +
								repeat("<", v.getSize()) +
								"]" +
								repeat (">", v.getSize()) + // we are at var offset
						// move to original : = (here = offset + variable offset) move - (offset + variable offset)
								repeat(">",nb);
					} while (false);
					break;
				case AROTL:
					do {
						Variable v = getVar(ope.asmOpeValue);
						nb = offset + v.getOffset();
						// move to pos :  = offset + variable offset -2
						ope.BF = repeat("<",nb - 2) +
								
								
								// increment the pos of the variable
								 "+" +
						// if pos = variable size then pos = 0
								BF_DUP +
								">" + repeat("+",v.getSize()) + "<" +
								">[-<->]<" + 
								BF_BOOL + 
								BF_NOT +
								"[-<[-]>]<" + 
								
/*								
								
								
						// decrement the pos of the variable
							//	 "-" +
						// if pos = 255 (-1) then pos = variable size-1
						//DUp BOOL NOT 
						"[>+>+<<-]>[<+>-]>[<+>-][-]<[>+<[-]]>[<+>-][-]<[>+<[-]]>[<+>-]<->[-]<[>+<[-]]>[<+>-]<" +
								//BF_DUP +
							//	BF_BOOL + 
							//	BF_NOT +
								"[-<[-]"+ repeat("+",v.getSize() )  + ">]<-" + 
*/							
								
						// goto left temporary
								"<" + 
						// clear the temporary
								"[-]" +
						// "variable size" time transfer right to here, moveright
								repeat("<[->+<]",v.getSize()) +
						// Transfer right temporary to offset
								repeat(">", v.getSize()) +
								"[-" +
								repeat("<", v.getSize()) +
								"+" +
								repeat(">", v.getSize()) +
								"]" +
							//	"<" + // we are at var offset
						// move to original : = (here = offset + variable offset) move - (offset + variable offset)
								repeat(">",nb-1);
					} while (false);
					break;
				case ASET:
					break;
				case ARESET:
					break;
				case DUP:
					ope.BF = BF_DUP;
					offset++;
					break;
				case DROP:
					ope.BF = BF_DROP;
					offset--;
					break;
				case SWAP:
					ope.BF = ">[-]>[-]<<[>+<-]<[>>>+<<<-]>>[<<+>>-]>[<<+>>-]<<";
					break;
				case WHILE:
				case LOOP:
					sbm.nextLevel();
					ope.BF = "[";
					compile(ope.opeList,offset,level+1,false);
					break;
				case AT:
					switch (ope.asmOpeType) {
					case FROMSTACKBM:
						nb = sbm.getBookmark(ope.asmOpeValue);
						if (nb == -1) {
							ope.errorMsg += "Unknown stack bookmark name '" +  ope.asmOpeValue + "'; ";
						} else {
							nb = offset-nb;
							if (nb>=0) {
								ope.BF =repeat("<",nb);
								compile(ope.opeList,offset,level+1, true);
								ope.BF_post =repeat(">",nb);
							} else {
								ope.errorMsg += "Out of scope stack bookmark '" +  ope.asmOpeValue + "'; ";
							}
						}
					break;
					case FROMSTACK:
						nb = Integer.parseInt(ope.asmOpeValue);
						ope.BF =repeat("<",nb);
						compile(ope.opeList,offset,level+1,true);
						ope.BF_post =repeat(">",nb);
						break;
					case VARNAME:
						Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)\\.(pos|POS)$");
						Matcher m = p.matcher(ope.asmOpeValue);
						Variable v = getVar(ope.asmOpeValue);
						if (m.matches()) { // is array pos
							String varName = m.group(1);
							v = getVar(varName);
							nb = v.getOffset() - 2 + offset;
							if (v.getSize()==1) {
								ope.errorMsg += "Variable '" + varName +"' is not an array; ";
							}
						} else { // is not array pos
							v = getVar(ope.asmOpeValue);
							nb = v.getOffset() + offset;
						}
						ope.BF =repeat("<",nb);
						compile(ope.opeList,offset,level+1, true);
						ope.BF_post =repeat(">",nb);
						break;
					default:
						ope.BF="[Error]";
						break;
				}
					break;
				case ENDSCOPE:
					switch (ope.asmOpeType) {
						case LOOP:
							sbm.previousLevel();
							ope.BF="-]"+ BF_DROP;
							offset--;
							break;
						case WHILE:
							sbm.previousLevel();
							ope.BF="]"+ BF_DROP;
							offset--;
							break;
						case IF:
							sbm.previousLevel();
							ope.BF = BF_DROP;
							offset--;
							break;
						case AT:
							ope.BF = "";
							break;
						default:
							ope.BF="[Error]";
							break;
					}
					break;
				case IF:
					sbm.nextLevel();
					ope.BF = BF_BOOL + BF_DUP + "[";
					offset++;
					compile(ope.opeList,offset,level+1,false);
					ope.BF_post = "-]" + BF_DROP;
					offset--;
					break;
				case ELSE:
					ope.BF = BF_DUP + BF_NOT +"[";
					offset++;
					sbm.previousLevel();
					sbm.nextLevel();
					compile(ope.opeList,offset,level+1,false);
					
					ope.BF_post = "-]"+ BF_DROP;
					offset--;
					break;
			}
			if (level == 1 && i == 0) {
					ope.BF = variablesBF() + " " + ope.BF; // add variable in stack
			}
		}
		if (offsetBefore != offset) {
			ltree.get(0).errorMsg += "Unbalanced stack between line " +
									ltree.get(0).lineNum + " and " +
									ltree.get(ltree.size()-1).lineNum +"; ";
		}
	}
	
	
	private String variablesBF() {
		String result = "";
		Variable[] aVariables = new Variable[mVariables.size()];
		for (Variable v: mVariables.values()) {
			aVariables[v.getIndex()] = v;
		}
		int offset=0;
		for (Variable v : aVariables) {
			if (v.getSize() == 1) {  // single byte var
				v.setOffset(offset);
				offset++;
				result += ">";
			} else { // array byte var
				offset++; // tmp1 for position compute
				offset++; // tmp2 for position compute
				offset++; // position in rotation
				offset++; // tmpArray var for rol right
				result += ">>>>"; // tmp1,tmp2,position,tmpArray
				v.setOffset(offset); 
				offset+= v.getSize(); // varriable itself
				result += repeat(">",v.getSize());
				offset++; // tmpArray var for rol left
				result += ">"; // tmpArray
			}
		}
		if (result.length()>0) result = result.substring(1);
		return result; 
	}
	
	public boolean hasError() {
		return errorList.size()>0;
	}
	
	public List<String> getErrorList() {
		return errorList;
	}
	
	private boolean declareVar(String varNameDecl,Operation ope) {
		Pattern p = Pattern.compile("^([_A-Za-z][_A-Za-z0-9.]*)(\\*([0-9]+))?$");
		Matcher m = p.matcher(varNameDecl);
		String varName = "";
		int varSize = 1;
		if (m.matches()) {
			varName = m.group(1);
			if (mVariables.containsKey(varName)) {
				ope.errorMsg += "Already declared variable; ";
			}
			String strVarSize = m.group(3);
			if (strVarSize!= null && strVarSize.length()>0) {
				varSize = Integer.parseInt(strVarSize);
				if (varSize<1 || varSize>255) {
					ope.errorMsg += "Incorrect variable array size; ";
				}
			}
		} else {
			ope.errorMsg += "Bad variable declaration syntax; ";
			return false; // bad declaration
		}
		if (mVariables.containsKey(varName)) {
			return false; // already declared variable
		}
		Variable v = new Variable(varName,ope, mVariables.size(), varSize);
		
		mVariables.put(varName, v);
		return true; // new variable declared
	}
	
	private boolean useVar(String varName) {
		if (!mVariables.containsKey(varName)) {
			return false; // not declared variable
		}
		Variable aVar = mVariables.get(varName);
		aVar.addUse();
		return true;
	}
	
	private Variable getVar(String varName) {
		return mVariables.get(varName);
	}
	
	private List<Operation> toTree(List<Operation> oriList, int idx, int level) {
		List<Operation> list = new ArrayList<Operation>();
		while (idx < oriList.size()) {
			Operation curOpe = oriList.get(idx);
			if (	curOpe.asmToken == AsmToken.LOOP ||
					curOpe.asmToken == AsmToken.WHILE ||
					curOpe.asmToken == AsmToken.IF ||
					curOpe.asmToken == AsmToken.AT) {
				list.add(curOpe);
				curOpe.opeList = toTree(oriList,idx+1, level+1);
				idx+= curOpe.sizeDeep()+1;
				if (idx < oriList.size()) {
					Operation curOpe2 = oriList.get(idx);
					if (curOpe2.asmToken == AsmToken.ENDSCOPE) {
						if ((curOpe.asmToken == AsmToken.LOOP &&
							 curOpe2.asmOpeType == AsmOpeType.LOOP) ||
							(curOpe.asmToken == AsmToken.IF &&
							 curOpe2.asmOpeType == AsmOpeType.IF)	||
							(curOpe.asmToken == AsmToken.AT &&
							 curOpe2.asmOpeType == AsmOpeType.AT)	||
							(curOpe.asmToken == AsmToken.WHILE &&
							 curOpe2.asmOpeType == AsmOpeType.WHILE)) {
							list.add(curOpe2);
							idx++;
						} else {
							curOpe.errorMsg += "IF / LOOP / AT with wrong closing scope; ";
							curOpe2.errorMsg += "END-IF / END-LOOP / TA  with wrong matching instruction; ";
							idx++;
						}
					} else if (curOpe2.asmToken == AsmToken.ELSE) {
						if (curOpe.asmToken != AsmToken.IF) {
							curOpe2.errorMsg += "ELSE without matching IF;";
							curOpe.errorMsg += "LOOP can't accept ELSE instruction;";
						}
						list.add(curOpe2);
						curOpe2.opeList = toTree(oriList,idx+1, level+1);
						idx+= curOpe2.sizeDeep()+1;
						if (idx < oriList.size()) {
							Operation curOpe3 = oriList.get(idx);
							if (curOpe3.asmToken == AsmToken.ENDSCOPE) {
								if (curOpe3.asmOpeType != AsmOpeType.IF) {
									curOpe3.errorMsg += "Expect END-IF and not END-LOOP";
								}
								list.add(curOpe3);
								idx++;
							} else {
								curOpe3.errorMsg += "Only one ELSE per IF; ";
								idx++;
							}
						} else {
							curOpe2.errorMsg += "ELSE with No closing scope; ";
						}
					}
				} else {
					curOpe.errorMsg+="IF or LOOP with No closing scope; ";
				}
			} else if (curOpe.asmToken == AsmToken.ELSE ||
						curOpe.asmToken == AsmToken.ENDSCOPE) {
				if (level == 1) curOpe.errorMsg += "Unexpected ELSE/ENDIF/ENDLOOP/TA; ";
				return list;
			} else {
				if (curOpe.asmToken != AsmToken.EMPTY_LINE &&
					curOpe.asmToken != AsmToken.COMMENT &&
					curOpe.asmToken != AsmToken.UNKNOWN) {
					list.add(curOpe);
				}
				idx++;
			}
		}
		return list;
	}
	
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {


		InputStream in= new FileInputStream(new File("/home/devauem/projects/DCTSC-api/dev/workspace/BF/src/test.asm"));//new ByteArrayInputStream(sampleCode.getBytes());
		try {
			Assembler asm = new Assembler(in,"/home/devauem/projects/DCTSC-api/dev/workspace/BF/src/");
			if (asm.hasError()) {
				List<String> el = asm.getErrorList();
				System.out.println("Compilation Error");
				for (String l:el) {
					System.out.println(l);
				}
			} else {
				OutputStream BFout = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(BFout);
				asm.getBFCode(ps);
				String code = BFout.toString();
				//System.out.println(code);
				// take 2 inputs, if equal then output 1 , 0 otherwise 
//				String input = ",>,[<->-][-]<[>+<[-]]>[<+>-]<->[-]<[>+<[-]]>[<+>-]<.!AA";
				String input = ",[.-]!a";
				Engine eng= new Engine(code,new ByteArrayInputStream(input.getBytes()) ,null);
				eng.run();
				OutputStream out = eng.getOutput();
				//System.out.println("rawOptimze:\n" + asm.getBFOnly(true,80));

				System.out.println("status:"+eng.getStatus().toString());
				System.out.println("output:" + out.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
