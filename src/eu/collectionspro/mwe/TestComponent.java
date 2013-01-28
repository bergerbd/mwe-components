package eu.collectionspro.mwe;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.mwe.core.WorkflowContext;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.emf.mwe.core.lib.AbstractWorkflowComponent;
import org.eclipse.emf.mwe.core.monitor.ProgressMonitor;
import org.eclipse.emf.mwe.utils.Writer;
import eu.collectionspro.mwe.QVTOExecutor;

/**
 *  Testing component that should be used in MWE2 for Test Execution and possibly comparison.
 *  You must specify xmi's creating models. Input models MUST be in the order input parameters
 *  specified in tested transformation. You have to specify comparison models - their amount and 
 *  order MUST be same as output parameters of transformation. It is possible to use double quotes 
 *  if no comparison needed to skip creating expected result model.   
 *  
 * @author woxie
 *
 */
public class TestComponent extends AbstractWorkflowComponent{
	protected List<String> inputUris;
	protected List<String> comparisonUris;

	//tested transformation URI
	protected String transformationFile;
	/**
	 * qvt output dir parent
	 */
	protected String outputParentUri;
	/**
	 * Optional description of test. If not specified, default value
	 *  "TEST" is used 
	 */
	protected String testDescription;
	
	//hidded Comparators
	protected List<ModelComparator> modelComparators;
	private final String INPUT_PREFIX = "INPUT_";
	private final String RESULT_PREFIX = "RESULT_";
	private final String COMPARISON_PREFIX = "COMPARISON_";
	
	public TestComponent(){
		inputUris = new ArrayList<String>();
		comparisonUris = new ArrayList<String>();
		testDescription = "TEST";
	}
	
	/** Processes QVTExecutor subtransformation that MUST have only **/
	protected void fulfillSlots(List <String> transformationList, String slotPrefix, 
			WorkflowContext ctx, ProgressMonitor monitor, Issues issues){
		for( int i = 0 ; i < transformationList.size() ; i++){
			if(!transformationList.get(i).equals("")){
				String inputUri = transformationList.get(i);
				String outputSlot = slotPrefix + i;
	
				//creating 
				QVTOExecutor inputExecutor = new QVTOExecutor();
				inputExecutor.setTransformationFile(inputUri);
				inputExecutor.addOutputSlot(outputSlot);
				inputExecutor.checkConfiguration(issues);
				inputExecutor.invoke(ctx, monitor, issues);
			}
		}
	}
	
	protected void storeSlots(List <String> transformationList, String slotPrefix, 
			WorkflowContext ctx, ProgressMonitor monitor, Issues issues){
		for( int i = 0 ; i < inputUris.size() ; i++){
			if(!transformationList.get(i).equals("")){
				String slot = slotPrefix + i;
				Writer slotWriter = new Writer();
				slotWriter.setUri(outputParentUri + "/" + slotPrefix + "X/" + slot + ".xmi");
				slotWriter.setCloneSlotContents(false);
				slotWriter.setUseSingleGlobalResourceSet(false);
				slotWriter.setModelSlot(slot);
				slotWriter.checkConfiguration(issues);
				slotWriter.invoke(ctx, monitor, issues);
			}
		}
	}
	
	@Override
	protected void invokeInternal(WorkflowContext ctx, ProgressMonitor monitor,
			Issues issues) {
		QVTOExecutor testTransformationExecuter = new QVTOExecutor();
		testTransformationExecuter.setTransformationFile(transformationFile);
		fulfillSlots(inputUris, INPUT_PREFIX, ctx, monitor, issues);
		storeSlots(inputUris, INPUT_PREFIX, ctx, monitor, issues);
		
		//adding inout slots into test transformation
		for( int i = 0 ; i < inputUris.size() ; i++){
			String inputSlot = INPUT_PREFIX + i;
			testTransformationExecuter.addInputSlot(inputSlot);
		}
		
		//adding inout slot into test transformation
		for(int i = 0 ; i < comparisonUris.size() ; i++){
			String resultSlot = RESULT_PREFIX + i;
			testTransformationExecuter.addOutputSlot(resultSlot);
		}
		
		//check and process test
		testTransformationExecuter.checkConfiguration(issues);
		testTransformationExecuter.invoke(ctx, monitor, issues);
		
		//store result slots which count equals to comparison slots count
		storeSlots(comparisonUris, RESULT_PREFIX, ctx, monitor, issues);
		
		//create and store comparison models
		fulfillSlots(comparisonUris, COMPARISON_PREFIX, ctx, monitor, issues);
		storeSlots(comparisonUris, COMPARISON_PREFIX, ctx, monitor, issues);
				
		//make comparations between result and expected result(comparison)
		for(int i = 0 ; i < comparisonUris.size() ; i++){
			if(!comparisonUris.get(i).equals("")){
				String comparisonXMI = outputParentUri+"/" + COMPARISON_PREFIX + "X/" 
						+ COMPARISON_PREFIX + i + ".xmi";
				String resultXMI = outputParentUri+"/" + RESULT_PREFIX + "X/" 
						+ RESULT_PREFIX + i + ".xmi";
				ModelComparator comparator = new ModelComparator();
				comparator.setTestDescription(testDescription + i);
				comparator.setTestedURI(resultXMI);
				comparator.setResultURI(comparisonXMI);
				comparator.invoke(ctx, monitor, issues);
			}
		}

		System.out.println("Testing " + testDescription + " finisthed\n");
	}

	public void addQvtInput(final String inputUri){
		this.inputUris.add(inputUri);
	}
	
	public void addQvtComparison(final String comparisonUri){
		this.comparisonUris.add(comparisonUri);
	}
	
	public void setOutputParentUri(String uri){
		outputParentUri = uri;
	}
	
	/**
	 * @param description
	 */
	public void setTestDescription(String description){
		testDescription = description;
	}
	
	public void setTransformationFile(String transformationUri){
		transformationFile = transformationUri;
	}

	
	@Override
	public void checkConfiguration(Issues issues) {
		if(transformationFile == null || transformationFile.equals("")){
			issues.addError("Missing tested transformation URI");
		}
		if(outputParentUri == null || outputParentUri.equals("")){
			issues.addError("Missing output directory URI");
		}
	}
}