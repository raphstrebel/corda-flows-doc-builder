package com.github.lucacampanella.callgraphflows.staticanalyzer;

import static java.lang.reflect.Modifier.ABSTRACT;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.StartableByRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.AnnotationFilter;
import spoon.reflect.visitor.filter.NamedElementFilter;

public class AnalyzerWithModel {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzerWithModel.class);

	private static boolean drawArrows = true;

	protected CtModel model;

	public static String[] pathToSrc;

	protected String analysisName;

	protected ClassCallStackHolder currClassCallStackHolder = null;

	private Map<CtClass, AnalysisResult> classToAnalysisResultMap = new HashMap<>();

	public CtModel getModel() {
		return model;
	}

	public static void setDrawArrows(boolean drawArrows) {
		AnalyzerWithModel.drawArrows = drawArrows;
	}

	public static void setPathToSrc(String[] pathToSrc) {
		AnalyzerWithModel.pathToSrc = pathToSrc;
	}

	public <T> CtClass getClass(Class<T> klass) {
		final List<CtClass> results = model.getElements(new NamedElementFilter(CtClass.class, klass.getSimpleName()));
		for (CtClass ctClass : results) {
			if (ctClass.getName().equals(klass.getName())) {
				return (CtClass) ctClass;
			}
		}
		return null;
	}

	public AnalysisResult analyzeFlowLogicClass(Class klass) throws AnalysisErrorException, NotFoundException {
		final CtClass ctClass = getClass(klass);
		if (ctClass == null) {
			throw new IllegalArgumentException("The class is not in the model");
		}
		return analyzeFlowLogicClass(ctClass);
	}

	public AnalysisResult analyzeFlowLogicClass(CtClass klass) throws AnalysisErrorException, NotFoundException {

		if (classToAnalysisResultMap.containsKey(klass)) {
			//LOGGER.info("*** class {} already analyzed, using cached result", klass.getQualifiedName());
			return classToAnalysisResultMap.get(klass);
		}
		else {
			ClassPool pool = new ClassPool();
			CtClass flowLogicCtClass = pool.makeClass("net.corda.core.flows.FlowLogic", null);

			//if(!klass.isSubtypeOf(MatcherHelper.getTypeReference(FlowLogic.class))) {
			if (!klass.subtypeOf(flowLogicCtClass)) {
				LOGGER.info(
						"----------------------------------------------------------------------------------------------------------------------");

				//LOGGER.info("analyzing class : {}",klass.getName()
				//JavassistClassDeclaration classDeclaration = new JavassistClassDeclaration(klass, null);

				//LOGGER.info("spoon model packages : {}", model.getAllPackages());

				//LOGGER.info("class name : {}", klass.getSimpleName());
				//LOGGER.info("super class name : {}", klass.getSuperclass().getSimpleName());
				//LOGGER.info("super class children : {}", klass.getSuperclass().getDirectChildren());
				//LOGGER.info("super class class : {}", klass.getSuperclass().getClass());
				//LOGGER.info("super class parent : {}", klass.getSuperclass().getParent());
				//LOGGER.info("super class short repr : {}", klass.getSuperclass().getShortRepresentation());
				//LOGGER.info("super class ref types : {}", klass.getSuperclass().getReferencedTypes());
				//LOGGER.info("super class type erasure : {}", klass.getSuperclass().getTypeErasure());
				//LOGGER.info("super class qualif name : {}", klass.getSuperclass().getQualifiedName());

				//LOGGER.info("super class type declaration : {}", klass.getSuperclass().getTypeDeclaration()); // null
				//LOGGER.info("super class declaration : {}", klass.getSuperclass().getDeclaration()); // null
				//LOGGER.info("super class unbox : {}", klass.getSuperclass().unbox()); // same as getSuperclass()
				//LOGGER.info("super class modifiers : {}", klass.getSuperclass().getModifiers()); // []
				//LOGGER.info("super class type param declaration : {}", klass.getSuperclass().getTypeParameterDeclaration());//
                // null
				//LOGGER.info("super class declaring type : {}", klass.getSuperclass().getDeclaringType()); // null
				//LOGGER.info("super class all fields : {}", klass.getSuperclass().getAllFields()); // []
				//LOGGER.info("super class actual type args : {}", klass.getSuperclass().getActualTypeArguments()); // not
                // important
				//LOGGER.info("super class original src fragment : {}", klass.getSuperclass().getOriginalSourceFragment()); //
                // error
				//LOGGER.info("super class declared fields : {}", klass.getSuperclass().getDeclaredFields()); // []

				LOGGER.info(
						"----------------------------------------------------------------------------------------------------------------------");
				//throw new IllegalArgumentException("Class " +klass.getQualifiedName() +" doesn't extend FlowLogic");
			}
			//LOGGER.info("*** analyzing class {}", klass.getQualifiedName());

			LOGGER.info("statically analyzing class : {}", klass.getSimpleName());
			final CtMethod callMethod = StaticAnalyzerUtils.findCallMethod(klass);
			if (callMethod == null) {
				throw new AnalysisErrorException(klass, "No call method found");
			}

			//if (callMethod.isAbstract()) {
			if (callMethod.getModifiers() == ABSTRACT) {
				String exMessage = "Found only an abstract call method";
				//if (callMethod.getParent() instanceof CtClass) {
				//	exMessage += " in class " + ((CtClass) (callMethod).getParent()).getName();
				//}
				throw new AnalysisErrorException(klass, exMessage);
			}

			setCurrentAnalyzingClass(klass);

			AnalysisResult res = new AnalysisResult(ClassDescriptionContainer.fromClass(klass));

			//res.getClassDescription().setReturnType(StaticAnalyzerUtils.nullifyIfVoidTypeAndGetString(callMethod.getType()));
			res.getClassDescription().setReturnType(callMethod.getReturnType().getName());

			//LOGGER.info("call method body statements : {}", callMethod.getBody().getStatements());


			//final Branch interestingStatements = MatcherHelper.fromCtStatementsToStatements(
			//		callMethod.getBody().getStatements(), this);
			//res.setStatements(interestingStatements);

			//is it only a "container" flow with no initiating call or also calls initiateFlow(...)?
			final boolean isInitiatingFlow = false;
			//final boolean isInitiatingFlow = interestingStatements.getInitiateFlowStatementAtThisLevel().isPresent();

			LOGGER.debug("Contains initiate call? {}", isInitiatingFlow);
			if (isInitiatingFlow) {
				//CtClass initiatedFlowClass = getDeeperClassInitiatedBy(klass);
                //
				//if (initiatedFlowClass != null) {
				//	res.setCounterpartyClassResult(analyzeFlowLogicClass(initiatedFlowClass));
				//}
				//else {
				//	LOGGER.error("Class {} contains initiateFlow call, but can't find corresponding class", klass.getName());
				//}
				//if (drawArrows) {
				//	final boolean validProtocol =
				//			res.checkIfContainsValidProtocolAndSetupLinks();//check the protocol and draws possible links
				//	//LOGGER.info("Class {} contains valid protocol? {}", klass.getQualifiedName(), validProtocol);
				//}
				//else {
				//	//LOGGER.info("Set on not drawing arrows, the protocol is not figured out");
				//}
			}

			classToAnalysisResultMap.put(klass, res);
			return res;
		}
	}

	public void setCurrentAnalyzingClass(CtClass klass) throws NotFoundException {
		currClassCallStackHolder = ClassCallStackHolder.fromCtClass(klass);
	}

	public ClassCallStackHolder getCurrClassCallStackHolder() {
		return currClassCallStackHolder;
	}

	public List<CtClass> getClassesByAnnotation(Class annotationClass) throws IOException, NotFoundException {

		List<CtElement> elements = model.getElements(new AnnotationFilter<>(annotationClass));

		LOGGER.info("------------------------ GET CLASSES BY ANNOTATION ------------------------");
		//for (CtElement element : elements) {
		//	CtClassImpl e = (CtClassImpl) element;
		//
		//	//if(e.getSimpleName().equals("BulkIssueEventInitiator")) {
		//	//    LOGGER.info(" element name : {}", e.getSimpleName());
		//	//    LOGGER.info(" element class : {}", e.getClass());
		//	//    LOGGER.info(" element superclass : {}", e.getSuperclass());
		//	//    LOGGER.info(" element superclass class : {}", e.getSuperclass().getClass());
		//	//    LOGGER.info(" element superclass getTypeDeclaration : {}", e.getSuperclass().getTypeDeclaration());
		//	//}
		//
		//	//CtRef... to CtClassImpl ? -> check google?..
		//}

		//((CtClassImpl) elements.get(0)).getActualClass()

		ClassPool pool = new ClassPool(ClassPool.getDefault());
		pool.appendClassPath("./otherlib/slf4j-api-1.7.6.jar");
		CtClass ctClass = pool.get("org.slf4j.Logger");

		//ClassPool pool = ClassPool.getDefault();
		//CtClass ctClass = pool.makeClass(new FileInputStream(((CtClassImpl) elements.get(0)).getQualifiedName()));

		return elements.stream()
				.map(CtClass.class::cast)
				.collect(Collectors.toList());
	}

	public List<CtClass> getClassesToBeAnalyzed() throws IOException, NotFoundException {
		return getClassesByAnnotation(StartableByRPC.class);
	}


	public CtClass getDeeperClassInitiatedBy(CtClass initiatingClass)
			throws ClassNotFoundException, IOException, NotFoundException {
		final List<CtClass> generalInitiatedByList = getClassesByAnnotation(InitiatedBy.class);
		for (CtClass klass : generalInitiatedByList) {

			if(klass.getAnnotation(InitiatedBy.class) != null) {
				return klass;


			//Optional<CtAnnotation<? extends Annotation>> initiatedByAnnotationOptional =
			//		klass.getAnnotations().stream().filter(ctAnnotation -> {
			//			boolean result = false;
			//			try {
			//				result = ctAnnotation.getActualAnnotation().annotationType() == InitiatedBy.class;
			//			}
			//			catch (Exception e) {
			//				//LOGGER.warn("Couldn't retrieve real representation for annotation {} for class {}, " +
			//				//"continuing without analyzing this one", ctAnnotation, klass.getQualifiedName());
			//			}
			//			return result;
			//		}).findFirst();
			//if (initiatedByAnnotationOptional.isPresent()) {
			//	final CtExpression referenceToClass =
			//			initiatedByAnnotationOptional.get().getAllValues().get("value");

				//if (((CtFieldReadImpl) referenceToClass).getVariable().getDeclaringType() == null) {
					//LOGGER.warn("Couldn't retrieve declaration of class declared in the @InitiatedBy " +
					//        "annotation. Skipping this class in finding the responder flow " +
					//        "\nThis could result in a problem in the produced graph." +
					//        " \nDeclared reference: {} \nDeclaring class: {} " +
					//        "\nInitiatingClass {}", referenceToClass, klass, initiatingClass);
					//continue;
				//}

				//final CtClass correspondingInitiatingClass = (CtClass) ((CtFieldReadImpl) referenceToClass).getVariable()
				//		.getDeclaringType().getTypeDeclaration();

				//if ((correspondingInitiatingClass.getReference().isSubtypeOf(initiatingClass.getReference())
				//		|| initiatingClass.getReference().isSubtypeOf(correspondingInitiatingClass.getReference())) &&
				//		(deeperInitiatedByClass == null ||
				//				klass.getReference().isSubtypeOf(deeperInitiatedByClass.getReference()))) {
			}
			}

		return null;
	}

	//public CtClass getFurthestAwaySubclass(CtClass superClass) {
	//	List<CtClass> allClasses = model.getElements(new TypeFilter<>(CtClass.class));
	//
	//	CtClass furthestAway = superClass;
	//
	//	for (CtClass subClass : allClasses) {
	//		if (subClass.isSubtypeOf(superClass.getReference())
	//				&& subClass.isSubtypeOf(furthestAway.getReference())) {
	//			furthestAway = subClass;
	//		}
	//	}
	//
	//	return furthestAway;
	//}

	//public List<CtClass> getAllSubClassesIncludingThis(CtClass superClass) {
	//	List<CtClass> allClasses = model.getElements(new TypeFilter<>(CtClass.class));
	//
	//	return allClasses.stream().filter(klass -> klass.isSubtypeOf(superClass.getReference()))
	//			.collect(Collectors.toList());
	//}

	public String getAnalysisName() {
		return analysisName;
	}
}
