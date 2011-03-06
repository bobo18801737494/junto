package upenn.junto.app

/**
 * Copyright 2011 Jason Baldridge
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

import scala.io.Source
import java.io._
import org.clapper.argot._



/** 
 * Read Junto output to get distributions for just words, and only
 * for labels that have higher probability than __DUMMY__.
 */
object OutputExtractor {

  val NodeRE = """([^_]+)_(.+)""".r
    
  val parser = new ArgotParser("Junto Output Extractor", preUsage=Some("Junto 1.1"))

  import ArgotConverters._

  val typeToExtractOption = 
    parser.option[String](List("t", "type"), "nodetype",
                               "The type of node to extract information for. E.g. if you have WORD_hello, then use -t WORD to get the distribution for WORD_hello and not FOO_hello.")


  val input = parser.parameter[String]("input",
				       "Input file to read. If not " +
				       "specified, use stdin.",
				       false)

  val output = parser.parameter[String]("outputfile",
                                        "Output file to which to write.",
                                        false)

  def main(args: Array[String]) = {
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message)
    }

    val typeToExtract = typeToExtractOption.value.get

    val outputFile = new FileWriter(new File(output.value.get))
    
    for (line <- Source fromFile(new File(input.value.get)) getLines()) {
      
      val Array(nodename, gold, injected, estimated, isTestNode, mrr) = line.split('\t')
      val NodeRE(nodetype,nodeval) = nodename
      
      if (nodetype == typeToExtract && estimated != "") {
        val estimatedList = estimated.split(" ")
        val (tags,probs) = 
          (for (i <- List.range(0,estimatedList.length,2)) 
             yield Pair(estimatedList(i),estimatedList(i+1).toDouble)).unzip
        
        var dummyIndex = tags.indexOf("__DUMMY__")
	if (dummyIndex == -1)
	  dummyIndex = tags.length

        val activeTags = tags.slice(0,dummyIndex)
        var activeProbs = probs.slice(0,dummyIndex)

        activeProbs = activeProbs.map{_/activeProbs.sum}
        
        outputFile.write(nodeval)

        for ((tag,prob) <- activeTags.zip(activeProbs))
          outputFile.write(" " + tag + " " + prob)

        outputFile.write("\n")
        
      }
    }
    outputFile.flush
    outputFile.close
  }
}




