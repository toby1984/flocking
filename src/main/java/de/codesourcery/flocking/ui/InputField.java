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

public class InputField<T extends Number> extends JPanel
{
    private static final int SLIDER_RESOLUTION = 400;
    
    private final JTextField textField;
    private final JSlider slider;
    
    private final boolean onlyIntValues;
    private final double minValue;
    private final double maxValue;
    
    private IModel<T> model;
    
    private boolean selfTriggeredEvent = false;
    
    public interface IModel<T> {
        public T getObject();
        public void setObject(T value);
    }
    
    public InputField(String label , IModel<T> m , double minValue,double maxValue,final boolean onlyIntValues) 
    {
        if ( m == null ) {
            throw new IllegalArgumentException("model must not be NULL.");
        }
        
        this.model = m;
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
                        textField.setText( numberToString( model.getObject() ) );
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
                
                final double range = Math.abs( InputField.this.maxValue - InputField.this.minValue );
                final double newValue = InputField.this.minValue + range*percentage;
                
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
        
        final InputField<Double> c = new InputField<Double>( "Test: " , numberModel , 0 , 100 , false );
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
    
    public void setModel(IModel<T> model) 
    {
        if (model == null) {
            throw new IllegalArgumentException("model must not be NULL.");
        }
        this.model = model;
        modelChanged();
    }
        
}
