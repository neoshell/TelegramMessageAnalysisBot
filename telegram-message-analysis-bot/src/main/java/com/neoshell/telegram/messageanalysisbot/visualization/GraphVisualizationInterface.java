package com.neoshell.telegram.messageanalysisbot.visualization;

import org.jgrapht.Graph;
import com.neoshell.telegram.messageanalysisbot.User;
import com.neoshell.telegram.messageanalysisbot.handler.ReplyStats;

public interface GraphVisualizationInterface {

  /**
   * Generates an image file to visualize the reply relationships.
   * 
   * @param graph
   * @param explicitReplyEdgeColor
   * @param implicitReplyEdgeColor
   * @param imageFilePath
   *          The path of the output image.
   * @throws Exception
   */
  public void visualizeReplyRelationship(Graph<User, ReplyStats> graph,
      String explicitReplyEdgeColor, String implicitReplyEdgeColor,
      String imageFilePath) throws Exception;

}
