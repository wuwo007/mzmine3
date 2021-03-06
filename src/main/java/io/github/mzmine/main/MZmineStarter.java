/*
 * Copyright 2006-2016 The MZmine 3 Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 3; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.main;

import java.io.File;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableList;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;

/**
 * MZmine modules support class
 */
public final class MZmineStarter implements Runnable {

    private static final File MODULES_FILE = new File("conf/Modules.xml");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static Map<Class<? extends MZmineModule>, MZmineModule> initializedModules = Collections
            .synchronizedMap(new Hashtable<>());

    @Override
    public void run() {

        logger.info("Loading modules");

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                    .newInstance();
            Document modulesDocument = null;
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            modulesDocument = dBuilder.parse(MODULES_FILE);
            Element rootElement = modulesDocument.getDocumentElement();
            NodeList moduleNodes = rootElement.getChildNodes();
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                Node moduleNode = moduleNodes.item(i);
                if (moduleNode.getNodeName() != "module")
                    continue;
                String moduleClassName = moduleNode.getTextContent();
                try {

                    // Start up the module
                    startModule(moduleClassName);

                } catch (Exception e) {
                    logger.warn("Failed to initialize module class "
                            + moduleClassName + ": " + e);
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            logger.error("Could not load modules from " + MODULES_FILE);
            System.exit(1);
        }

        try {
            logger.info("Loading configuration");
            MZmineCore.getConfiguration()
                    .loadConfiguration(MZmineConfiguration.CONFIG_FILE);
        } catch (Exception e) {
            logger.error("Could not load configuration from "
                    + MZmineConfiguration.CONFIG_FILE);
        }

    }

    private void startModule(final String moduleClassName)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {

        logger.info("Starting module class " + moduleClassName);

        // Create an instance of the module
        @SuppressWarnings("unchecked")
        Class<? extends MZmineModule> moduleClass = (Class<? extends MZmineModule>) Class
                .forName(moduleClassName);
        MZmineModule newModule = moduleClass.newInstance();
        initializedModules.put(moduleClass, newModule);

        // Create a parameter set
        Class<? extends ParameterSet> parameterSetClass = newModule
                .getParameterSetClass();
        ParameterSet newParameterSet = parameterSetClass.newInstance();
        MZmineCore.getConfiguration().setModuleParameters(moduleClass,
                newParameterSet);
    }

    static List<MZmineModule> getAllModules() {
        List<MZmineModule> list = ImmutableList
                .copyOf(initializedModules.values());
        return list;
    }

    /**
     * Returns the instance of a module of given class
     */
    @SuppressWarnings("unchecked")
    static <ModuleType extends MZmineModule> ModuleType getModuleInstance(
            Class<ModuleType> moduleClass) {
        return (ModuleType) initializedModules.get(moduleClass);
    }

}
