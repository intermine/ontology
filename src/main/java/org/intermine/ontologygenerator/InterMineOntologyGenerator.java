package org.intermine.ontologygenerator;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import org.apache.jena.vocabulary.XSD;
import org.intermine.metadata.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;



public class InterMineOntologyGenerator
{
	public final static String interMineVocNS = "http://intermine.org/vocabulary/";
    public final static String interMineOntologyURI = "http://intermine.org/ontology/intermine.owl";

	public static void main(String[] args) {
		OntModel ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		setKnownPrefixes(ontologyModel);
		setBasicInfo(ontologyModel);
		Set<ClassDescriptor> classDescriptors =
			org.intermine.metadata.Model.getInstanceByName("genomic").getClassDescriptors();
		for (ClassDescriptor cd : classDescriptors) {
			generateOntologyClass(cd, ontologyModel);
		}
		try {
			String ontologyFile = "doc/intermine.owl";
			PrintWriter out = new PrintWriter(new FileWriter(ontologyFile));
			ontologyModel.write(out, "RDF/XML");
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static void setKnownPrefixes(final org.apache.jena.rdf.model.Model jenaModel) {
		jenaModel.setNsPrefix("rdfs", RDFS.uri);
        jenaModel.setNsPrefix("dc", DCTerms.getURI());
	}

    private static void setBasicInfo(OntModel ontologyModel) {
        Ontology ontology = ontologyModel.createOntology(interMineOntologyURI);
        ontology.addProperty(DCTerms.title, "InterMine core model ontology");
        ontology.addProperty(RDFS.seeAlso, "http://intermine.org/");
        ontology.addProperty(DCTerms.description, "Simple ontology for description of classes"
                + " and their attributes defined in intermine core model");
        ontology.addProperty(DCTerms.license, "https://creativecommons.org/licenses/by/4.0/");
    }

	private static void generateOntologyClass(ClassDescriptor cd, OntModel ontologyModel) {
		String className = cd.getSimpleName();
		if (!className.equalsIgnoreCase("InterMineObject")) {
			OntClass ontologyClass = ontologyModel.createClass(interMineVocNS + className);
			for (FieldDescriptor fd : cd.getFieldDescriptors()) {
				if (fd.isAttribute() && !"id".equals(fd.getName())) {
					generateProperty((AttributeDescriptor) fd, ontologyClass, ontologyModel);
				} else if (fd.isReference() || fd.isCollection()) {
					generateReference((ReferenceDescriptor) fd, ontologyClass, ontologyModel);
				}
			}
			for (ClassDescriptor subClassDescriptor : cd.getSubDescriptors()) {
				OntClass subOntologyClass = ontologyModel.createClass(interMineVocNS + subClassDescriptor.getSimpleName());
                subOntologyClass.addProperty(RDFS.subClassOf, ontologyClass);
			}
		}
	}

	private static final void generateProperty(AttributeDescriptor attributeDescriptor, OntClass ontologyClass, OntModel ontologyModel) {
		ObjectProperty property = ontologyModel.createObjectProperty(interMineVocNS
				+ "has" + StringUtils.capitalize(attributeDescriptor.getName()));
		property.setDomain(ontologyClass);
		try {
			property.setRange(getRange(Class.forName(attributeDescriptor.getType())));
		} catch (ClassNotFoundException ex) {
			System.out.println("The type " + attributeDescriptor.getType() + " defined in the model is not valid");
		}
	}

	private static final void generateReference(ReferenceDescriptor referenceDescriptor, OntClass ontologyClass, OntModel ontologyModel) {
		ObjectProperty property = ontologyModel.createObjectProperty(interMineVocNS
				+ "has" + StringUtils.capitalize(referenceDescriptor.getName()));
		property.setDomain(ontologyClass);
		String refClassName = referenceDescriptor.getReferencedClassName();
		String simpleRefClassName = refClassName.substring(refClassName.lastIndexOf(".") + 1);
		property.setRange(ontologyModel.createClass(interMineVocNS + simpleRefClassName));
	}

	private static final  Resource getRange(Class<?> clazz) {
		if (clazz.equals(Integer.class) || clazz.equals(Integer.TYPE)) {
			return XSD.xint;
		}
		if (clazz.equals(Boolean.class) || clazz.equals(Boolean.TYPE)) {
			return XSD.xboolean;
		}
		if (clazz.equals(Double.class) || clazz.equals(Double.TYPE)) {
			return XSD.xdouble;
		}
		if (clazz.equals(Float.class) || clazz.equals(Float.TYPE)) {
			return XSD.xfloat;
		}
		if (clazz.equals(Long.class) || clazz.equals(Long.TYPE)) {
			return XSD.xlong;
		}
		if (clazz.equals(Short.class) || clazz.equals(Short.TYPE)) {
			return XSD.xshort;
		}
		if (clazz.equals(Byte.class) || clazz.equals(Byte.TYPE)) {
			return XSD.xbyte;
		}
		if (clazz.equals(Character.class) || clazz.equals(Character.TYPE)) {
			return XSD.xstring;
		}
		if (clazz.equals(Date.class)) {
			return XSD.date;
		}
		if (clazz.equals(BigDecimal.class)) {
			return XSD.decimal;
		}
		if (clazz.equals(String.class)) {
			return XSD.xstring;
		}
		return XSD.xstring;
	}
}
