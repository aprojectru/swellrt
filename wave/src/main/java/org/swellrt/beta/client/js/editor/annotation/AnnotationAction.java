package org.swellrt.beta.client.js.editor.annotation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.swellrt.beta.client.js.JsUtils;
import org.swellrt.beta.client.js.editor.SEditorException;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

/**
 * Action to be performed in a set of annotations for a provided range.
 * 
 * TODO rewrite in a stream-based way, current impl is a mess.
 * 
 *  @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class AnnotationAction {
  
  Set<ParagraphAnnotation> paragraphAnnotations = new HashSet<ParagraphAnnotation>(); 
  Set<TextAnnotation> textAnnotations = new HashSet<TextAnnotation>(); 
  JsoStringSet textAnnotationsNames = JsoStringSet.create();

  final Range range;
  final Editor editor;
     
  boolean deepTraverse = false;
  boolean projectEffective = false;
  
  public AnnotationAction(Editor editor, Range range) {
    this.range = range;
    this.editor = editor;
  }
     
  public void deepTraverse(boolean enabled) {
    this.deepTraverse = enabled;
  }
  
  public void add(JsArrayString names) throws SEditorException {
    for (int i = 0; i < names.length(); i++)
      add(names.get(i));
  }
  
  public void add(String nameOrPrefix) throws SEditorException {
    Preconditions.checkArgument(nameOrPrefix != null && !nameOrPrefix.isEmpty(),
        "Annotation name or prefix not provided");

    if (!nameOrPrefix.contains("/")) { // it's name
      addByName(nameOrPrefix);
    } else { // it's prefix
      for (String name : AnnotationRegistry.getNames()) {
        if (name.startsWith(nameOrPrefix)) {
          addByName(name);
        }
      }

    }
  }
  

  /**
   * Configure this annotation action to only consider effective annotations for its range.
   * <p>
   * Within a range, there could exist multiple instances of the same annotation spanning
   * subranges. An effective annotation must span at least the size of the range.   
   * 
   * @param projectEffective enable projections o
   */
  public void onlyEffectiveAnnotations(boolean enable) {
    this.projectEffective = enable;
  }
  
  protected void addByName(String name) throws SEditorException {
    Annotation antn = AnnotationRegistry.get(name);
    if (antn != null) {
     
      if (antn instanceof TextAnnotation) {
        textAnnotations.add((TextAnnotation) antn);
        textAnnotationsNames.add(((TextAnnotation) antn).getName()); // ensure we use canonical name
      } else if (antn instanceof ParagraphAnnotation) {
        paragraphAnnotations.add((ParagraphAnnotation) antn);
      }
      
    }  else{
      throw new SEditorException("Invalid annotation name");
    }
  }
  
  protected void addAllAnnotations() {
    AnnotationRegistry.forEach(new BiConsumer<String, Annotation>() {

      @Override
      public void accept(String t, Annotation u) {
        
        if (u instanceof TextAnnotation) {
          textAnnotations.add((TextAnnotation) u);
          textAnnotationsNames.add(t);
        } else if (u instanceof ParagraphValueAnnotation) {
          paragraphAnnotations.add((ParagraphAnnotation) u);
        }
        
      }
    });
  }
  
  public void reset() {
    
    boolean getAll = textAnnotations.isEmpty() && paragraphAnnotations.isEmpty();
    
    if (getAll) 
      addAllAnnotations();
    
    // Text annotations
    TextAnnotation.clearRange(editor.getDocument(), editor.getContent().getLocalAnnotations(), editor.getCaretAnnotations(), textAnnotationsNames, range.getStart(), range.getEnd());
    
    // Paragraph annotations
    ParagraphAnnotation.clearRange(editor.getContent(), range, paragraphAnnotations);
  }
  

  
  protected JsoView createResult() {
    return JsoView.as(JavaScriptObject.createObject());
  }
  
  protected void addToResult(JsoView result, String key, AnnotationInstance instance) {
    
    //
    // Remove prefix of annotation key to avoid javascript syntax errors
    // accessing properties e.g. "annotations.paragraph/header" is not supported
    //
    
    int slashPos = key.indexOf("/");
    if (slashPos >= 0)
      key = key.substring(slashPos+1);
                
    
    if (!projectEffective) {
      
      // Map of arrays, we expect multiple instances of the same annotation within a range.        
      
      JavaScriptObject arrayJso = result.getJso(key);      
      if (arrayJso == null) {
        arrayJso = JsoView.createArray().cast();
        result.setJso(key, arrayJso);
      }        
      JsUtils.addToArray(arrayJso, instance);
      
    } else {
      
      // on projecting effective annotations, if several instances
      // are found in the range, keep always the one with a non null value.
      boolean overwriteAnnotation = true;
         
      Object formerInstance = result.getObjectUnsafe(key);
      if (formerInstance != null) {
        AnnotationInstance typedFormerInstance = (AnnotationInstance) formerInstance;
        overwriteAnnotation = typedFormerInstance.value == null;
      }
      
      // A simple map, one annotation instance per key     
      if (overwriteAnnotation)
        result.setObject(key, instance);        
    }
  }
  
  
  /** 
   * @return annotations object 
   */
  public JavaScriptObject get() {
    
    JsoView result = createResult();

    boolean getAll = textAnnotations.isEmpty() && paragraphAnnotations.isEmpty();
    
    if (getAll) 
      addAllAnnotations();
    
    //
    // Text annotations
    // 
    
    if (range.isCollapsed()) {
      
      for (TextAnnotation antn: textAnnotations) {
        String value = editor.getDocument().getAnnotation(range.getStart(), antn.getName());
        Range actualRange = EditorAnnotationUtil.getEncompassingAnnotationRange(editor.getDocument(), antn.getName(), range.getStart());
        if (value != null)
          addToResult(result, antn.getName(), AnnotationInstance.create(editor.getContent(), antn.getName(), value, actualRange, AnnotationInstance.MATCH_IN)); 
      }
      
    } else {
     
      //
      // Within a range, there could exist multiple instances of the same annotation. 
      // If projectEffective, only consider those spanning at least the full range.
     
      editor.getDocument().rangedAnnotations(range.getStart(), range.getEnd(), textAnnotationsNames).forEach(new Consumer<RangedAnnotation<String>>() {
        @Override
        public void accept(RangedAnnotation<String> t) {
          Range anotRange = new Range(t.start(), t.end());          
          int matchType = AnnotationInstance.getRangeMatch(range, anotRange);            
           
          if (projectEffective && matchType != AnnotationInstance.MATCH_IN)
            return;
          
          if (t.value() != null)
              addToResult(result, t.key(), AnnotationInstance.create(editor.getContent(), t.key(), t.value(), anotRange, matchType));          
        }          
      });
    }
    
    //
    // Paragraph annotations
    // 
    
    if (!deepTraverse) {

      Paragraph.traverse(editor.getContent().getLocationMapper(), range.getStart(), range.getEnd(),
          new ContentElement.Action() {

            @Override
            public void execute(ContentElement e) {

              for (Annotation antn : paragraphAnnotations) {

                if (antn instanceof ParagraphValueAnnotation) {
                  String name = ((ParagraphValueAnnotation) antn).getName();
                  String value = ((ParagraphValueAnnotation) antn).apply(e);
                  if (value != null)
                    addToResult(result, name, AnnotationInstance.create(name, value, range, e, AnnotationInstance.MATCH_IN));
                }
              }

            }

          });

    } else {

      Paragraph.traverseDoc(editor.getDocument(), new ContentElement.RangedAction() {
        
        @Override
        public void execute(ContentElement e, Range r) {
          
          for (Annotation antn : paragraphAnnotations) {
  
            if (antn instanceof ParagraphValueAnnotation) {
              String name = ((ParagraphValueAnnotation) antn).getName();
              String value = ((ParagraphValueAnnotation) antn).apply(e);
              if (value != null)
                addToResult(result, name, AnnotationInstance.create(name, value, r, e, AnnotationInstance.MATCH_IN));
            }
          }
          
        }
        
      });
    
    }
    
    return result;
  }
  
}
