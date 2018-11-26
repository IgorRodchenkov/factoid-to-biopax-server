package factoid.converter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonReader;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

//TODO: test each distinct json template (cases 1 - 4E from the interaction types doc.)
public class FactoidToBiopaxTest {

  @Test
  public void testToOwl() throws IOException {
    Gson gson = new Gson();
    JsonReader reader = new JsonReader(new FileReader(getClass()
      .getResource("/test2.json").getFile()));
    JsonArray templates = gson.fromJson(reader, JsonArray.class);
    FactoidToBiopax converter = new FactoidToBiopax();
    converter.addToModel(templates);
    String res = converter.convertToBiopax();

    assertThat(res,notNullValue());
    assertThat(res, endsWith("</rdf:RDF>"));

    Model m = converterResultToModel(res);
    assertThat(m, notNullValue());
    assertThat(m.getObjects().size(), greaterThan(20));

    Set<SmallMoleculeReference> smols = m.getObjects(SmallMoleculeReference.class);
    assertThat(smols.size(), is(2));

    Set<Modulation> mods = m.getObjects(Modulation.class);
    assertThat(mods.size(), is(2));

    List<?> controllers = mods.stream().map(Modulation::getController)
      .flatMap(Set::stream).collect(Collectors.toList());
    assertThat(controllers.stream().filter(SmallMolecule.class::isInstance).count(), is(2L));
  }
  
  @Test
  public void testMolecularInteraction() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Molecular Interaction\",\n" +
		  "    \"participants\": [\n" +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"BMP2\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": P12643,\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"LEP\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": \"P41159\",\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      }\n" +
		  "    ]\n" +
		  "  }]";

	  FactoidToBiopax converter = new FactoidToBiopax();
	  converter.addToModel(templates); //processing

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(7));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), equalTo("uniprot"));
		  assertThat(x.getId(), isOneOf("P41159","P12643"));
	  });
	  
	  Set<MolecularInteraction> intns = m.getObjects(MolecularInteraction.class);
	  assertThat(intns, not(empty()));
	  MolecularInteraction intn = intns.iterator().next();
	  Set<Entity> participants = intn.getParticipant();

	  assertThat(participants.size(), equalTo(2));
	  participants.stream().forEach(p -> {
		  assertThat(p, instanceOf(Protein.class));
		  assertThat(((Protein)p).getEntityReference(), notNullValue());
		  assertThat(((Protein)p).getEntityReference().getXref(), not(empty()));
	  });
  }
  
  @Test
  public void testExpressionRegulation() throws IOException {
	  String templates = "[{\n" +
			  "    \"type\": \"Expression Regulation\",\n" +
			  "    \"controlType\": \"activation\",\n" +
			  "    \"modification\": \"phosphorylated\",\n" +
			  "    \"controller\": " +
			  "      {\n" +
			  "        \"type\": \"protein\",\n" +
			  "        \"name\": \"BMP2\",\n" +
			  "        \"xref\": {\n" +
			  "          \"id\": P12643,\n" +
			  "          \"db\": \"uniprot\"\n" +
			  "        }\n" +
			  "      },\n" +
			  "    \"target\": " +
			  "      {\n" +
			  "        \"type\": \"protein\",\n" +
			  "        \"name\": \"LEP\",\n" +
			  "        \"xref\": {\n" +
			  "          \"id\": \"P41159\",\n" +
			  "          \"db\": \"uniprot\"\n" +
			  "        }\n" +
			  "      }\n" +
			  "  }]";

		  FactoidToBiopax converter = new FactoidToBiopax();
		  converter.addToModel(templates); //processing

		  Model m = converterResultToModel(converter.convertToBiopax());
		  assertThat(m.getObjects().size(), equalTo(8));

		  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
		  assertThat(xrefs, notNullValue());
		  assertThat(xrefs.size(), equalTo(2));
		  xrefs.stream().forEach(x -> {
			  assertThat(x.getDb(), equalTo("uniprot"));
			  assertThat(x.getId(), isOneOf("P41159","P12643"));
		  });  
		  
		  Set<TemplateReactionRegulation> controls = m.getObjects(TemplateReactionRegulation.class);
		  assertThat(controls, not(empty()));
		  TemplateReactionRegulation control = controls.iterator().next();
		  
		  assertThat(control.getControlType(), equalTo(ControlType.ACTIVATION));
		  assertThat(control.getController(), not(empty()));
		  assertThat(control.getControlled(), not(empty()));
		  
		  TemplateReaction reaction = (TemplateReaction) control.getControlled().iterator().next();
		  PhysicalEntity controller = (PhysicalEntity) control.getController().iterator().next();
		  
		  assertThat(reaction.getParticipant(), not(empty()));
		  
		  PhysicalEntity product = (PhysicalEntity) reaction.getParticipant().iterator().next();
		  
		  assertThat(product, instanceOf(Protein.class));
		  assertThat(controller, instanceOf(Protein.class));
		  
		  assertThat(((Protein) product).getEntityReference(), notNullValue());
		  assertThat(((Protein) controller).getEntityReference(), notNullValue());
		  
		  assertThat(((Protein) product).getEntityReference().getXref(), not(empty()));
		  assertThat(((Protein) controller).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testProteinControlsState() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Protein Controls State\",\n" +
		  "    \"controlType\": \"activation\",\n" +
		  "    \"modification\": \"phosphorylated\",\n" +
		  "    \"controller\": " +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"BMP2\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": P12643,\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "    \"target\": " +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"LEP\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": \"P41159\",\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      }\n" +
		  "  }]";

	  FactoidToBiopax converter = new FactoidToBiopax();
	  converter.addToModel(templates); //processing

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(13));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), equalTo("uniprot"));
		  assertThat(x.getId(), isOneOf("P41159","P12643"));
	  });  
	  
	  Set<Catalysis> controls = m.getObjects(Catalysis.class);
	  assertThat(controls, not(empty()));
	  Catalysis control = controls.iterator().next();
	  
	  assertThat(control.getControlled(), not(empty()));
	  assertThat(control.getController(), not(empty()));
	  assertThat(control.getControlType(), equalTo(ControlType.ACTIVATION));
	  
	  Conversion conversion = (Conversion) control.getControlled().iterator().next();
	  assertThat(conversion.getLeft(), not(empty()));
	  assertThat(conversion.getRight(), not(empty()));
	  
	  PhysicalEntity controller = (PhysicalEntity) control.getController().iterator().next();
	  PhysicalEntity left = (PhysicalEntity) conversion.getLeft().iterator().next();
	  PhysicalEntity right = (PhysicalEntity) conversion.getRight().iterator().next();
	  
	  assertThat(controller, instanceOf(Protein.class));
	  assertThat(left, instanceOf(Protein.class));
	  assertThat(right, instanceOf(Protein.class));
	  
	  assertThat(((Protein) left).getEntityReference(), notNullValue());
	  assertThat(((Protein) controller).getEntityReference(), notNullValue());
	  assertThat(((Protein) right).getEntityReference(), equalTo(((Protein) left).getEntityReference()));
	  
	  assertThat(((Protein) left).getEntityReference().getXref(), not(empty()));
	  assertThat(((Protein) controller).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testConversion() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Other Interaction\",\n" +
		  "    \"controlType\": \"activation\",\n" +
		  "    \"participants\": [\n" +
		  "      {\n" +
		  "        \"type\": \"chemical\",\n" +
		  "        \"name\": \"Estradiol\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": 9811784,\n" +
		  "          \"db\": \"pubchem\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "      {\n" +
		  "        \"type\": \"chemical\",\n" +
		  "        \"name\": \"Progesterone\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": 5994,\n" +
		  "          \"db\": \"pubchem\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "    ]\n" +
		  "  }]";
	  
	  FactoidToBiopax converter = new FactoidToBiopax();
	  converter.addToModel(templates); //processing

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(7));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), equalTo("pubchem"));
		  assertThat(x.getId(), isOneOf("5994","9811784"));
	  });
	  
	  Set<Conversion> conversions = m.getObjects(Conversion.class);
	  assertThat(conversions, not(empty()));
	  Conversion conversion = conversions.iterator().next();
	  
	  assertThat(conversion.getLeft(), not(empty()));
	  assertThat(conversion.getRight(), not(empty()));
	  
	  PhysicalEntity left = (PhysicalEntity) conversion.getLeft().iterator().next();
	  PhysicalEntity right = (PhysicalEntity) conversion.getRight().iterator().next();
	  
	  assertThat(left, instanceOf(SmallMolecule.class));
	  assertThat(right, instanceOf(SmallMolecule.class));
	  
	  assertThat(left.getDisplayName(), equalTo("Estradiol"));
	  assertThat(right.getDisplayName(), equalTo("Progesterone"));
	  
	  assertThat(((SmallMolecule) left).getEntityReference(), notNullValue());
	  assertThat(((SmallMolecule) right).getEntityReference(), notNullValue());
	  
	  assertThat(((SmallMolecule) left).getEntityReference().getXref(), not(empty()));
	  assertThat(((SmallMolecule) right).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testControlsConsumtionOrProduction() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Other Interaction\",\n" +
		  "    \"controlType\": \"activation\",\n" +
		  "    \"participants\": [\n" +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"BMP2\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": P12643,\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "      {\n" +
		  "        \"type\": \"chemical\",\n" +
		  "        \"name\": \"Progesterone\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": 5994,\n" +
		  "          \"db\": \"pubchem\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "    ]\n" +
		  "  }]";

	  FactoidToBiopax converter = new FactoidToBiopax();
	  converter.addToModel(templates); //processing

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(8));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), isOneOf("uniprot", "pubchem"));
		  assertThat(x.getId(), isOneOf("5994","P12643"));
	  });
	  
	  Set<Catalysis> controls = m.getObjects(Catalysis.class);
	  assertThat(controls, not(empty()));
	  Catalysis control = controls.iterator().next();
	  assertThat(control.getControlType(), nullValue());
	  
	  assertThat(control.getController(), not(empty()));
	  assertThat(control.getControlled(), not(empty()));
	  
	  PhysicalEntity controller = (PhysicalEntity) control.getController().iterator().next();
	  BiochemicalReaction controlled = (BiochemicalReaction) control.getControlled().iterator().next();
	  
	  assertThat(controlled.getLeft(), empty());
	  assertThat(controlled.getRight(), not(empty()));
	  
	  PhysicalEntity target = (PhysicalEntity) controlled.getRight().iterator().next();
	  
	  assertThat(controller, instanceOf(Protein.class));
	  assertThat(target, instanceOf(SmallMolecule.class));
	  
	  assertThat(((Protein)controller).getEntityReference(), notNullValue());
	  assertThat(((SmallMolecule)target).getEntityReference(), notNullValue());

	  assertThat(((Protein)controller).getEntityReference().getXref(), not(empty()));
	  assertThat(((SmallMolecule)target).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testControlSequence() throws IOException {
	  String templates = "[{\n" +
	      "    \"type\": \"Other Interaction\",\n" +
	      "    \"controlType\": \"activation\",\n" +
	      "    \"participants\": [\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"BMP2\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": P12643,\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      },\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"LEP\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": \"P41159\",\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      }\n" +
	      "    ]\n" +
	      "  }]";
	
    FactoidToBiopax converter = new FactoidToBiopax();
    converter.addToModel(templates); //processing

    Model m = converterResultToModel(converter.convertToBiopax());
    assertThat(m.getObjects().size(), equalTo(8));

    Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
    assertThat(xrefs, notNullValue());
    assertThat(xrefs.size(), equalTo(2));
    xrefs.stream().forEach(x -> {
      assertThat(x.getDb(), equalTo("uniprot"));
      assertThat(x.getId(), isOneOf("P41159","P12643"));
    });  
    
    Set<Control> controls = m.getObjects(Control.class);
    controls.stream().forEach(c -> {
    		assertThat(c.getControlType(), equalTo(ControlType.ACTIVATION));
    		assertThat(c.getController().size(), equalTo(1));
    		assertThat(c.getController().iterator().next(), instanceOf(Protein.class));
    		
    		assertThat(c.getControlled().size(), isOneOf(0, 1));
    		if (c.getControlled().iterator().hasNext()) {
    			assertThat(c.getControlled().iterator().next(), instanceOf(Control.class));
    		}
    });
    
    Set<Protein> entities = m.getObjects(Protein.class);
    entities.stream().forEach(e -> {
		assertThat(e.getEntityReference(), notNullValue());
	    assertThat(e.getEntityReference().getXref(), not(empty()));
    });
  }
  
  @Test
  public void testGeneralInteraction() throws IOException {
	  String templates = "[{\n" +
	      "    \"type\": \"Other Interaction\",\n" +
	      "    \"participants\": [\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"BMP2\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": P12643,\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      },\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"LEP\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": \"P41159\",\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      }\n" +
	      "    ]\n" +
	      "  }]";
	
    FactoidToBiopax converter = new FactoidToBiopax();
    converter.addToModel(templates); //processing

    Model m = converterResultToModel(converter.convertToBiopax());
    assertThat(m.getObjects().size(), equalTo(7));

    Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
    assertThat(xrefs, notNullValue());
    assertThat(xrefs.size(), equalTo(2));
    xrefs.stream().forEach(x -> {
      assertThat(x.getDb(), equalTo("uniprot"));
      assertThat(x.getId(), isOneOf("P41159","P12643"));
    });  
    
    Set<Interaction> intns = m.getObjects(Interaction.class);
    assertThat(intns, not(empty()));
    Interaction intn = intns.iterator().next();
    Set<Entity> participants = intn.getParticipant();
    
    assertThat(participants.size(), equalTo(2));
    participants.stream().forEach(p -> {
    		assertThat(p, instanceOf(Protein.class));
    		assertThat(((Protein)p).getEntityReference(), notNullValue());
    	    assertThat(((Protein)p).getEntityReference().getXref(), not(empty()));
    });
  }

  @Test
  //Progesterone chemical-affects LEP (case 4D) is about activation of LEP protein (catalyst) by Progesterone.
  public void testChemicalAffects() throws IOException {
    String templates = "[{\n" +
      "    \"type\": \"Other Interaction\",\n" +
      "    \"controlType\": \"activation\",\n" +
      "    \"participants\": [\n" +
      "      {\n" +
      "        \"type\": \"chemical\",\n" +
      "        \"name\": \"Progesterone\",\n" +
      "        \"xref\": {\n" +
      "          \"id\": 5994,\n" +
      "          \"db\": \"pubchem\"\n" +
      "        }\n" +
      "      },\n" +
      "      {\n" +
      "        \"type\": \"protein\",\n" +
      "        \"name\": \"LEP\",\n" +
      "        \"xref\": {\n" +
      "          \"id\": \"P41159\",\n" +
      "          \"db\": \"uniprot\"\n" +
      "        }\n" +
      "      }\n" +
      "    ]\n" +
      "  }]";

    FactoidToBiopax converter = new FactoidToBiopax();
    converter.addToModel(templates); //processing

    Model m = converterResultToModel(converter.convertToBiopax());
    assertThat(m.getObjects().size(), equalTo(8));

    Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
    assertThat(xrefs, notNullValue());
    assertThat(xrefs.size(), equalTo(2));
    xrefs.stream().forEach(x -> {
      assertThat(x.getDb(), isOneOf("pubchem","uniprot"));
      assertThat(x.getId(), isOneOf("P41159","5994"));
    });

    Set<Modulation> mos = m.getObjects(Modulation.class);
    assertThat(mos, not(empty()));
    Modulation mod = mos.iterator().next();
    assertThat(mod.getControlType(), is(ControlType.ACTIVATION));
    assertThat(mod.getDisplayName(), nullValue()); //would be nice to generate "Progesterone activates production of LEP"
    assertThat(mod.getController().size(), equalTo(1));
    Controller con = mod.getController().iterator().next();
    assertThat(con, instanceOf(SmallMolecule.class));
    assertThat(con.getXref(), empty());
    assertThat(((SmallMolecule)con).getEntityReference(), notNullValue());
    assertThat(((SmallMolecule)con).getEntityReference().getXref(), not(empty()));
    assertThat(mod.getControlled().size(), equalTo(1));
    Process proc = mod.getControlled().iterator().next();
    assertThat(proc, instanceOf(Catalysis.class));
    Catalysis cat = (Catalysis) proc;
    assertThat(cat.getController().size(), equalTo(1));
    con = cat.getController().iterator().next();
    assertThat(con, instanceOf(Protein.class));
    assertThat(con.getXref(), empty());
    assertThat(((Protein)con).getEntityReference(), notNullValue());
    assertThat(((Protein)con).getEntityReference().getXref(), not(empty()));
    assertThat(cat.getControlType(), is(ControlType.ACTIVATION));
    //controlled reaction is unknown
    assertThat(cat.getControlled(), empty());
  }

  //local utils

  private Model converterResultToModel(String result)
    throws UnsupportedEncodingException
  {
    return (new SimpleIOHandler()).convertFromOWL(new ByteArrayInputStream(
      result.getBytes("UTF-8")));
  }

}
