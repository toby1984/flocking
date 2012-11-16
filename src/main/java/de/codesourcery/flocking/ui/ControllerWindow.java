package de.codesourcery.flocking.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import de.codesourcery.flocking.ui.InputField.IModel;

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
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
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
        setVisible( true );
    }
    
    public static void main(String[] args)
    {
        new ControllerWindow(SimulationParameters.getDefaultParameters()) {

            @Override
            protected void parametersChanged(SimulationParameters newParams)
            {
            }

            @Override
            protected void rendererChanged(boolean useOpenGL)
            {
            }

            @Override
            protected void vsyncChanged(boolean vsyncEnabled)
            {
            }
        };
    }
    
    private SimulationParameters getSimulationParameters() {
        return new SimulationParameters(populationSize, modelMax, maxSteeringForce, maxSpeed, cohesionWeight,
                separationWeight, alignmentWeight, borderForceWeight, separationRadius, neighbourRadius, borderRadius);
    }

    protected abstract void parametersChanged(SimulationParameters newParams);
    
    protected abstract void rendererChanged(boolean useOpenGL);
    
    protected abstract void vsyncChanged(boolean vsyncEnabled);
        
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
        
        panel.add( new InputField<Integer>("Population:" , new PropertyModel<Integer>( ControllerWindow.this , "populationSize" ) , 10 , 50000 , true) ,
                createConstraints(0,y++) );
        
        panel.add( new InputField<Double>("Model max:" , new PropertyModel<Double>( ControllerWindow.this , "modelMax" ) , 1000 , 10000 , true) ,
                createConstraints(0,y++) );       
        
        panel.add( new InputField<Double>("Max. steering force:" , new PropertyModel<Double>( ControllerWindow.this , "maxSteeringForce" ) , 0 , 20 , false) ,
                createConstraints(0,y++) );   
        
        panel.add( new InputField<Double>("Max. speed:" , new PropertyModel<Double>( ControllerWindow.this , "maxSpeed" ) , 1 , 50 , false) ,
                createConstraints(0,y++) );
        
        // weights
        panel.add( new InputField<Double>("Cohesion weight:" , new PropertyModel<Double>( ControllerWindow.this , "cohesionWeight" ) , 0 , 2 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new InputField<Double>("Separation weight:" , new PropertyModel<Double>( ControllerWindow.this , "separationWeight" ) , 0 , 2 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new InputField<Double>("Alignment weight:" , new PropertyModel<Double>( ControllerWindow.this , "alignmentWeight" ) , 0 , 2 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new InputField<Double>("Border force weight:" , new PropertyModel<Double>( ControllerWindow.this , "borderForceWeight" ) , 0 , 2 , false) ,
                createConstraints(0,y++) );      
        
        // radiuses
        panel.add( new InputField<Double>("Neighbor radius:" , new PropertyModel<Double>( ControllerWindow.this , "neighbourRadius" ) , 0 , 100 , false) ,
                createConstraints(0,y++) );  
        
        panel.add( new InputField<Double>("Separation radius:" , new PropertyModel<Double>( ControllerWindow.this , "separationRadius" ) , 0 , 100 , false) ,
                createConstraints(0,y++) );          
        
        panel.add( new InputField<Double>("Border radius:" , new PropertyModel<Double>( ControllerWindow.this , "borderRadius" ) , 0 , 100 , false) ,
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
    
    protected final class PropertyModel<T> implements IModel<T> {

        private final Object target;
        private final String property;
        
        protected PropertyModel(Object target, String property)
        {
            this.target = target;
            this.property = property;
        }

        private Field getField() 
        {
            Class<?> current = target.getClass();
            while ( current != null && current != Object.class ) 
            {
                for ( Field f : current.getDeclaredFields() ) 
                {
                    if ( f.getName().equals( property ) ) 
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
            
            throw new RuntimeException("Failed to find suitable field named '"+property+"' in "+target);
        }
        
        @Override
        public T getObject()
        {
            try {
                return (T) getField().get(target);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException("Failed to read field '"+property+"' of "+target,e);                
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
                    if ( field.getType() == Integer.TYPE || field.getType() == Integer.class ) {
                        valueToSet = ((Number) value).intValue();
                    }
                    if ( field.getType() == Long.TYPE || field.getType() == Long.class ) {
                        valueToSet = ((Number) value).longValue();
                    }                    
                } 
                
                field.set(target, valueToSet);
                
                parametersChanged( getSimulationParameters() );
            } 
            catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException("Failed to write field '"+property+"' of "+target,e);  
            } 
        }
    }    
}
