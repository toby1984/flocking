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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;

/**
 * Compound Swing input component for numbers.
 * 
 * <p>This component is made up of a label, a textfield
 * and a slider. It allows the user to enter an integer 
 * and/or floating-point number by either typing the
 * digits in the textbox or adjusting a slider.
 * Minimum/Maximum values are enforced.
 * </p> 
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class NumberInputField<T extends Number> extends JPanel
{
	/**
	 * Slider resolution.
	 * 
	 * The higher the value, the more fine-grained 
	 * the adjustments are.
	 */
    public static final int SLIDER_RESOLUTION = 400;
    
    private final JTextField textField;
    private final JSlider slider;
    
    // whether the user may enter floating-point values or not
    private final boolean onlyIntValues;
    private final double minValue;
    private final double maxValue;
    
    private volatile IModel<T> model;
    
    // flag used to prevent triggering recursive ActionListener
    // invocations when the textfield adjusts the slider or vice versa
    private boolean selfTriggeredEvent = false;
    
    /**
     * Input model used to exchange user input with 
     * client code.
     * 
     * <p>I shamelessly stole the 'model' idea from
     * the awesome Apache Wicket framework.</p>
     *
     * @author tobias.gierke@code-sourcery.de
     */
    public interface IModel<T> 
    {
        public T getObject();
        
        public void setObject(T value);
    }
    
    /**
     * Create instance.
     *  
     * <p>Creates a resizable panel that holds a label, a textfield and a slider
     * for entering/adjusting a numeric value.</p>
     * 
     * @param label the label to display
     * @param model the model that is used to read/write the value to be edited. If the model returns <code>null</code> values,
     * these will be treated as "0" (or "0.0" respectively).
     * @param minValue valid minimum value (inclusive) the user may enter
     * @param maxValue vali maximum value (inclusive) the user may enter
     * @param onlyIntValues whether the user may enter only integers or integers <b>and</b> floating-point numbers. 
     */
    public NumberInputField(String label , IModel<T> model , double minValue,double maxValue,final boolean onlyIntValues) 
    {
        if ( model == null ) {
            throw new IllegalArgumentException("model must not be NULL.");
        }
        
        this.model = model;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onlyIntValues = onlyIntValues;
        
        textField = new JTextField("0");
        textField.setColumns( 5 );
        textField.setHorizontalAlignment( JTextField.RIGHT );
        
        slider = new JSlider(0 , SLIDER_RESOLUTION );
        
        textField.addActionListener( new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if ( selfTriggeredEvent ) {
                    return;
                }
                
                final String s = textField.getText();
                if ( ! StringUtils.isBlank( s ) ) 
                {
                    Number number = null;
                    try 
                    {
                        if ( onlyIntValues ) {
                            number = Long.parseLong( s.trim() );
                        } else {
                            number = Double.parseDouble( s.trim() );
                        }
                    } 
                    catch(Exception ex) 
                    {
                        textField.setText( numberToString( NumberInputField.this.model.getObject() ) );
                        return;
                    }

                    updateModelValue(number  );
                }
            }
        });
        
        textField.setText( numberToString( model.getObject() ) );
        
        slider.getModel().setValue( calcSliderValue( model.getObject() ) );
        slider.addChangeListener( new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e)
            {
                if ( selfTriggeredEvent ) 
                {
                    return;
                }
                final double percentage = slider.getModel().getValue() / (double) SLIDER_RESOLUTION; // 0...1
                
                final double range = Math.abs( NumberInputField.this.maxValue - NumberInputField.this.minValue );
                final double newValue = NumberInputField.this.minValue + range*percentage;
                
                updateModelValue( newValue );
            }
        });
        
        slider.setMinimumSize( new Dimension(100,20));        
        slider.setPreferredSize( new Dimension(100,20));
        
        // do layout
        setLayout( new GridBagLayout() );
        
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.NONE;
        cnstrs.weightx = 0;
        cnstrs.weighty = 0;
        cnstrs.gridx=0;
        cnstrs.gridy=0;
        
        final JLabel l = new JLabel(label);
        l.setMinimumSize( new Dimension(1500,20));        
        l.setPreferredSize( new Dimension(150,20));        
        l.setVerticalAlignment(SwingConstants.TOP );
        add( l ,cnstrs );        
        
        cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.NONE;
        cnstrs.weightx = 0;
        cnstrs.weighty = 0;
        cnstrs.gridx=1;
        cnstrs.gridy=0;
        cnstrs.insets = new Insets( 0 , 0 , 0 , 10 );
        
        add( textField ,cnstrs );    
        
        cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.HORIZONTAL;
        cnstrs.weightx = 1.0;
        cnstrs.weighty = 1.0;
        cnstrs.gridx=2;
        cnstrs.gridy=0;
        
        add( slider ,cnstrs );        
    }
    
    // testing only
    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        final IModel<Double> numberModel = new IModel<Double>() {
            
            private double value;
            
            @Override
            public void setObject(Double object)
            {
                System.out.println("Value: "+object);
                this.value = object;
            }
            
            @Override
            public Double getObject()
            {
                return value;
            }
        };
        
        final NumberInputField<Double> c = new NumberInputField<Double>( "Test: " , numberModel , 0 , 100 , false );
        frame.getContentPane().add( c );
        frame.pack();
        frame.setVisible( true );
    }
    
    @SuppressWarnings("unchecked")
    private void updateModelValue(Number n) 
    {
        if ( onlyIntValues ) {
            model.setObject( (T) limitInt( n ) );
        } else {
            model.setObject( (T) limitDouble( n ) );
        }
        modelChanged();
    }
    
    /**
     * To be invoked when this input components
     * underlying {@link IModel} has changed
     * and thus this component needs repainting.
     */
    public void modelChanged() {
    
        final Number n = model.getObject();
        
        final int sliderValue = calcSliderValue(n);
        
        final Runnable r = new Runnable() {

            @Override
            public void run()
            {
                selfTriggeredEvent = true;
                slider.setValue( sliderValue );
                textField.setText( numberToString( n ) );
                selfTriggeredEvent = false;
            }
        }; 
        
        if ( SwingUtilities.isEventDispatchThread() ) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait( r );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int calcSliderValue(final Number n)
    {
        return (int) Math.round( calcPercentageValue(n)*SLIDER_RESOLUTION );
    }
    
    private double calcPercentageValue(final Number n)
    {
        final double abs = Math.abs( maxValue - minValue );
        final double rel;
        
        if ( onlyIntValues ) {
            long value = limitInt( n );
            rel = Math.abs( value - minValue );            
        } else {
            final double value = limitDouble( n );
            rel = Math.abs( value - minValue );
        }
        
        return rel / abs;
    }
    
    private Long limitInt(Number n) 
    {
        final long value = n == null ? 0 : n.longValue();
        if ( value < minValue ) {
            return (long) minValue;
        } 
        if ( value > maxValue ) {
            return (long) maxValue;
        }
        return value;
    }
    
    private Double limitDouble(Number n) 
    {
        final double value = n == null ? 0 : n.doubleValue();
        if ( value < minValue ) {
            return minValue;
        } 
        if ( value > maxValue ) {
            return maxValue;
        }
        return value;
    }
    
    protected String numberToString(Number n) 
    {
        if ( onlyIntValues ) {
            return n == null ? "0" : Long.toString( n.intValue() );
        }
        final DecimalFormat DF = new DecimalFormat("########0.0###");
        return n == null ? "0.0" :DF.format( n.doubleValue() );
    }
    
    /**
     * Sets a new model for this input component.
     * 
     * @param model
     * @see #modelChanged()
     */
    public void setModel(IModel<T> model) 
    {
        if (model == null) {
            throw new IllegalArgumentException("model must not be NULL.");
        }
        this.model = model;
        modelChanged();
    }
        
}
