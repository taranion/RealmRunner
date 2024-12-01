package de.log4j.mudclient;

import java.io.StringWriter;

import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.MainConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class ConfigTest {

	public ConfigTest() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		Config config = new Config();
		config.setServer("rom.mud.de");
		config.setPort(4000);

		MainConfig main = new MainConfig();
		main.addWorld("rom", config);

		DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        // Fix below - additional configuration
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		Representer representer = new Representer(options) {
		    @Override
		    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,Tag customTag) {
		        // if value of property is null, ignore it.
		        if (propertyValue == null) {
		            return null;
		        }
		        else {
		            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
		        }
		    }
		};
		representer.addClassTag(MainConfig.class, Tag.MAP);

		Yaml yaml = new Yaml(representer);
		StringWriter out = new StringWriter();
		yaml.dump(main, out);
		System.out.println(out.toString());
	}

}
