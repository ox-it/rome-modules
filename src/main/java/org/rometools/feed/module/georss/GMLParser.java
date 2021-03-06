/*
 * Copyright 2006 Marc Wick, geonames.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.rometools.feed.module.georss;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jdom2.Element;
import org.rometools.feed.module.georss.geometries.Envelope;
import org.rometools.feed.module.georss.geometries.LineString;
import org.rometools.feed.module.georss.geometries.LinearRing;
import org.rometools.feed.module.georss.geometries.Point;
import org.rometools.feed.module.georss.geometries.Polygon;
import org.rometools.feed.module.georss.geometries.Position;
import org.rometools.feed.module.georss.geometries.PositionList;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;

/**
 * GMLParser is a parser for the GML georss format.
 * 
 * @author Marc Wick
 * @version $Id: GMLParser.java,v 1.2 2007/06/05 20:44:53 marcwick Exp $
 * 
 */
public class GMLParser implements ModuleParser {

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.syndication.io.ModuleParser#getNamespaceUri()
     */
    @Override
    public String getNamespaceUri() {
        return GeoRSSModule.GEORSS_GEORSS_URI;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.syndication.io.ModuleParser#parse(org.jdom2.Element)
     */
    @Override
    public Module parse(final Element element, final Locale locale) {
        final Module geoRssModule = parseGML(element);
        return geoRssModule;
    }

    private static PositionList parsePosList(final Element element) {
        final String coordinates = element.getText();
        final String[] coord = GeoRSSUtils.trimWhitespace(coordinates).split(" ");
        final PositionList posList = new PositionList();
        for (int i = 0; i < coord.length; i += 2) {
            posList.add(Double.parseDouble(coord[i]), Double.parseDouble(coord[i + 1]));
        }
        return posList;
    }

    static Module parseGML(final Element element) {
        GeoRSSModule geoRSSModule = null;

        final Element pointElement = element.getChild("Point", GeoRSSModule.GML_NS);
        final Element lineStringElement = element.getChild("LineString", GeoRSSModule.GML_NS);
        final Element polygonElement = element.getChild("Polygon", GeoRSSModule.GML_NS);
        final Element envelopeElement = element.getChild("Envelope", GeoRSSModule.GML_NS);
        if (pointElement != null) {
            final Element posElement = pointElement.getChild("pos", GeoRSSModule.GML_NS);
            if (posElement != null) {
                geoRSSModule = new GMLModuleImpl();
                final String coordinates = posElement.getText();
                final String[] coord = GeoRSSUtils.trimWhitespace(coordinates).split(" ");
                final Position pos = new Position(Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));
                geoRSSModule.setGeometry(new Point(pos));
            }
        } else if (lineStringElement != null) {
            final Element posListElement = lineStringElement.getChild("posList", GeoRSSModule.GML_NS);
            if (posListElement != null) {
                geoRSSModule = new GMLModuleImpl();
                geoRSSModule.setGeometry(new LineString(parsePosList(posListElement)));
            }
        } else if (polygonElement != null) {
            Polygon poly = null;

            // The external ring
            final Element exteriorElement = polygonElement.getChild("exterior", GeoRSSModule.GML_NS);
            if (exteriorElement != null) {
                final Element linearRingElement = exteriorElement.getChild("LinearRing", GeoRSSModule.GML_NS);
                if (linearRingElement != null) {
                    final Element posListElement = linearRingElement.getChild("posList", GeoRSSModule.GML_NS);
                    if (posListElement != null) {
                        if (poly == null) {
                            poly = new Polygon();
                        }
                        poly.setExterior(new LinearRing(parsePosList(posListElement)));
                    }
                }
            }

            // The internal rings (holes)
            final List interiorElementList = polygonElement.getChildren("interior", GeoRSSModule.GML_NS);
            final Iterator it = interiorElementList.iterator();
            while (it.hasNext()) {
                final Element interiorElement = (Element) it.next();
                if (interiorElement != null) {
                    final Element linearRingElement = interiorElement.getChild("LinearRing", GeoRSSModule.GML_NS);
                    if (linearRingElement != null) {
                        final Element posListElement = linearRingElement.getChild("posList", GeoRSSModule.GML_NS);
                        if (posListElement != null) {
                            if (poly == null) {
                                poly = new Polygon();
                            }
                            poly.getInterior().add(new LinearRing(parsePosList(posListElement)));
                        }
                    }
                }

            }

            if (poly != null) {
                geoRSSModule = new GMLModuleImpl();
                geoRSSModule.setGeometry(poly);
            }
        } else if (envelopeElement != null) {
            final Element lowerElement = envelopeElement.getChild("lowerCorner", GeoRSSModule.GML_NS);
            final Element upperElement = envelopeElement.getChild("upperCorner", GeoRSSModule.GML_NS);
            if (lowerElement != null && upperElement != null) {
                geoRSSModule = new GMLModuleImpl();
                final String lowerCoordinates = lowerElement.getText();
                final String[] lowerCoord = GeoRSSUtils.trimWhitespace(lowerCoordinates).split(" ");
                final String upperCoordinates = upperElement.getText();
                final String[] upperCoord = GeoRSSUtils.trimWhitespace(upperCoordinates).split(" ");
                final Envelope envelope = new Envelope(Double.parseDouble(lowerCoord[0]), Double.parseDouble(lowerCoord[1]), Double.parseDouble(upperCoord[0]),
                        Double.parseDouble(upperCoord[1]));
                geoRSSModule.setGeometry(envelope);
            }
        }

        return geoRSSModule;
    }

}
