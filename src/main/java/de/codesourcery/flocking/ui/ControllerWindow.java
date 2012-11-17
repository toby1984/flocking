/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.flocking.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import de.codesourcery.flocking.SimulationParameters;
import de.codesourcery.flocking.ui.NumberInputField.IModel;

/**
 * Abstract {@link JFrame} subclass that displays
 * the user-interface to adjust simulation parameters on-the-fly.  
 *
 * <p>Subclasses need to implement various callback-methods that
 * are triggered whenever the user changes simulation parameters.</p>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class ControllerWindow extends JFrame
{
    // =========== simulation params ==========
    
    private int populationSize;    
    private double modelMax;
    
    private double maxSteeringForce;
    private double maxSpeed;
    
    private double cohesionWeight;
    private double separationWeight;
    private double alignmentWeight;
    private double borderForceWeight;
    
    private double separationRadius;
    private double neighbourRadius;
    private double borderRadius;
 
    // ================= UI elements ========
    
    public ControllerWindow(SimulationParameters p)
    {
        setSimulationParameters(p);
        
        // setup layout
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        addWindowListener( new WindowAdapter() {
        	public void windowClosed(WindowEvent e) {
        		onDispose();
        	}
        } );
        
        final JPanel panel = new JPanel();
        panel.setLayout( new GridBagLayout() );
        
        layoutPanel( panel );
        
        // add panel 
        getContentPane().setLayout( new GridBagLayout() );
        final GridBagConstraints cnstrs = createConstraints( 0,  0 );
        cnstrs.fill = GridBagConstraints.BOTH;
        cnstrs.weightx =1.0;
        cnstrs.weighty =1.0;
        getContentPane().add( panel , cnstrs );
        pack();
    }
    
    // test main
    public static void main(String[] args)
    {
    	final ControllerWindow window = new ControllerWindow(SimulationParameters.getDefaultParameters()) {

            @Override
            protected void parametersChanged(SimulationParameters newParams) { }

            @Override
            protected void rendererChanged(boolean useOpenGL) { }

            @Override
            protected void vsyncChanged(boolean vsyncEnabled) { }

			@Override
			protected void onDispose() { }
        };
        window.setVisible(true);
    }
    
    private SimulationParameters getSimulationParameters() 
    {
        return new SimulationParameters(populationSize, modelMax, maxSteeringForce, maxSpeed, cohesionWeight,
                separationWeight, alignmentWeight, borderForceWeight, separationRadius, neighbourRadius, borderRadius);
    }

    /**
     * Invoked whenever a simulation parameter has changed.
     * 
     * @param newParams
     */
    protected abstract void parametersChanged(SimulationParameters newParams);
    
    /**
     * Invoked when the user selects a different renderer.
     * 
     * @param useOpenGL whether the OpenGL renderer should be used.
     */
    protected abstract void rendererChanged(boolean useOpenGL);
    
    /**
     * Invoked whenever the user toggls the vertical-sync setting.
     * 
     * @param vsyncEnabled
     */
    protected abstract void vsyncChanged(boolean vsyncEnabled);
    
    /**
     * Invoked when the user closes this window.
     */
    protected abstract void onDispose();
        
    private void layoutPanel(JPanel panel) 
    {
        int y = 0;
        
        // renderer radio buttons
        ButtonGroup group = new ButtonGroup();
        
        final JRadioButton java2DButton = new JRadioButton("Java2D" );
        java2DButton.setSelected( true );
        
        final JRadioButton openGLButton = new JRadioButton("OpenGL" );
        
        group.add( java2DButton );
        group.add( openGLButton );
        
        final ActionListener listener = new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if ( e.getSource() == java2DButton ) {
                    rendererChanged( false );
                } else {
                    rendererChanged( true );
                }
            }
        };
        
        java2DButton.addActionListener( listener );
        openGLButton.addActionListener( listener );

        JPanel buttonPanel = new JPanel();
        buttonPanel.add( new JLabel("Renderer:"));
        buttonPanel.add( java2DButton );
        buttonPanel.add( openGLButton );
        
        // vsync checkbox
        final JCheckBox checkbox = new JCheckBox("VSync: " , true );
        checkbox.setHorizontalTextPosition(SwingConstants.LEFT);
        checkbox.addActionListener( new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                vsyncChanged( checkbox.isSelected() );
            }
        });
        
        buttonPanel.add( checkbox );
        
        GridBagConstraints cnstrs = createConstraints( 0 ,y++ );
        cnstrs.fill=GridBagConstraints.NONE;
        cnstrs.anchor=GridBagConstraints.WEST;
        cnstrs.weightx=0;
        panel.add( buttonPanel , cnstrs );        
        
        panel.add( new NumberInputField<Integer>("Population:" , new PropertyModel<Integer>( ControllerWindow.this , "populationSize" ) , 10 , 50000 , true) ,
                createConstraints(0,y++) );
        
        panel.add( new NumberInputField<Double>("Scaling:" , new PropertyModel<Double>( ControllerWindow.this , "modelMax" ) , 100 , 10000 , true) ,
                createConstraints(0,y++) );       
        
        panel.add( new NumberInputField<Double>("Max. steering force:" , new PropertyModel<Double>( ControllerWindow.this , "maxSteeringForce" ) , 0 , 10 , false) ,
                createConstraints(0,y++) );   
        
        panel.add( new NumberInputField<Double>("Max. speed:" , new PropertyModel<Double>( ControllerWindow.this , "maxSpeed" ) , 1 , 20 , false) ,
                createConstraints(0,y++) );
        
        // weights
        panel.add( new NumberInputField<Double>("Cohesion weight:" , new PropertyModel<Double>( ControllerWindow.this , "cohesionWeight" ) , 0 , 1 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new NumberInputField<Double>("Separation weight:" , new PropertyModel<Double>( ControllerWindow.this , "separationWeight" ) , 0 , 1 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new NumberInputField<Double>("Alignment weight:" , new PropertyModel<Double>( ControllerWindow.this , "alignmentWeight" ) , 0 , 1 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new NumberInputField<Double>("Border force weight:" , new PropertyModel<Double>( ControllerWindow.this , "borderForceWeight" ) , 0 , 1 , false) ,
                createConstraints(0,y++) );      
        
        // radiuses
        panel.add( new NumberInputField<Double>("Neighbor radius:" , new PropertyModel<Double>( ControllerWindow.this , "neighbourRadius" ) , 10 , 100 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new NumberInputField<Double>("Separation radius:" , new PropertyModel<Double>( ControllerWindow.this , "separationRadius" ) , 0 , 100 , false) ,
                createConstraints(0,y++) );          
        
        panel.add( new NumberInputField<Double>("Border radius:" , new PropertyModel<Double>( ControllerWindow.this , "borderRadius" ) , 0 , 500 , false) ,
                createConstraints(0,y++) ); 
    }

    private void setSimulationParameters(SimulationParameters p)
    {
        this.populationSize = p.populationSize;
        this.modelMax = p.modelMax;
        this.maxSteeringForce = p.maxSteeringForce;
        this.maxSpeed = p.maxSpeed;
        
        this.cohesionWeight = p.cohesionWeight;
        this.separationWeight = p.separationWeight;
        this.alignmentWeight = p.alignmentWeight;
        this.borderForceWeight = p.borderForceWeight;
                
        this.separationRadius = p.separationRadius;
        this.neighbourRadius = p.neighbourRadius;
        this.borderRadius = p.borderRadius;
    }
    
    private static GridBagConstraints createConstraints(int x,int y) 
    {
        final GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.HORIZONTAL;
        cnstrs.weightx = 1.0;
        cnstrs.weighty = 0.0;
        cnstrs.gridx=x;
        cnstrs.gridy=y;
        return cnstrs;
    }
    
    /**
     * An {@link IModel} implementation that uses reflection to
     * access private fields of a class.
     *
     * <p>This class was inspired by Apache Wickets <code>PropertyModel</code> class.</p>
     * <p>Note that this model only works for public/private/protected instance (non-static)
     * fields.</p>
     * 
     * @author tobias.gierke@code-sourcery.de
     */
    private final class PropertyModel<T> implements IModel<T> {

        private final Object target;
        private final String fieldName;
       
        /**
         * Create instance.
         * 
         * @param target target object whose field should be read/set by this model
         * @param fieldName Name of the instance field on the target object that should be manipulated
         */
        public PropertyModel(Object target, String fieldName)
        {
            this.target = target;
            this.fieldName = fieldName;
        }

        private Field getField() 
        {
            Class<?> current = target.getClass();
            while ( current != null && current != Object.class ) 
            {
                for ( Field f : current.getDeclaredFields() ) 
                {
                    if ( f.getName().equals( fieldName ) ) 
                    {
                        if ( ! Modifier.isFinal( f.getModifiers() ) && 
                             ! Modifier.isStatic( f.getModifiers() ) ) 
                        { 
                            f.setAccessible( true );                        
                            return f;
                        } 
                    }
                }
                current = current.getSuperclass();
            }
            throw new RuntimeException("Failed to find suitable field named '"+fieldName+"' in "+target);
        }
        
        @SuppressWarnings("unchecked")
		@Override
        public T getObject()
        {
            try {
                return (T) getField().get(target);
            } 
            catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException("Failed to read field '"+fieldName+"' of "+target,e);                
            }
        }

        @Override
        public void setObject(T value)
        {
            try 
            {
                final Field field = getField();
                
                Object valueToSet = value;
                if ( value instanceof Number) 
                {
                	// convert number to target type 
                    if ( field.getType() == Integer.TYPE || field.getType() == Integer.class ) {
                        valueToSet = ((Number) value).intValue();
                    }
                    if ( field.getType() == Float.TYPE || field.getType() == Float.class ) {
                        valueToSet = ((Number) value).floatValue();
                    }   
                    if ( field.getType() == Double.TYPE || field.getType() == Double.class ) {
                        valueToSet = ((Number) value).doubleValue();
                    }                    
                    if ( field.getType() == Long.TYPE || field.getType() == Long.class ) {
                        valueToSet = ((Number) value).longValue();
                    }                    
                } 
                
                field.set(target, valueToSet);
                
                parametersChanged( getSimulationParameters() );
            } 
            catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException("Failed to write field '"+fieldName+"' of "+target,e);  
            } 
        }
    }    
}
