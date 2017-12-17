package com.neoshell.telegram.messageanalysisbot.visualization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.AttributeType;
import org.jgrapht.io.ComponentAttributeProvider;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.DefaultAttribute;

import com.neoshell.telegram.messageanalysisbot.User;
import com.neoshell.telegram.messageanalysisbot.handler.ReplyStats;

public class Graphviz implements GraphVisualizationInterface {

  private static final double DEFAULT_NETWORK_EDGES_MIN_WIDTH = 1.0;
  private static final double DEFAULT_NETWORK_EDGES_MAX_WIDTH = 5.0;

  private static final String DEFAULT_GRAPHVIZ_FONT_NAME = "simsun";
  private static final String DEFAULT_GRAPHVIZ_OUTPUT_TYPE = "png";
  private static final String DEFAULT_GRAPHVIZ_REPRESENTATION_TYPE = "circo";
  private static final int DEFAULT_GRAPHVIZ_DPI = 96;

  private String graphvizPath;
  private String tempDir;

  public Graphviz(String graphvizPath, String tempDir) {
    this.graphvizPath = graphvizPath;
    this.tempDir = tempDir;
  }

  @Override
  public void visualizeReplyRelationship(Graph<User, ReplyStats> graph,
      String explicitReplyEdgeColor, String implicitReplyEdgeColor,
      String imageFilePath) throws Exception {
    double maxScore = 0.0;
    for (ReplyStats replyStats : graph.edgeSet()) {
      maxScore = Math.max(maxScore, replyStats.getScore());
    }
    DOTExporter<User, ReplyStats> dotExporter = new DOTExporter<>(
        new UserIdProvider(), new UserFullNameProvider(),
        new ReplyScoreProvider(), new UserVertexFormatProvider(),
        new ReplyStatsEdgeFormatProvider(maxScore, explicitReplyEdgeColor,
            implicitReplyEdgeColor));
    File dotFile = File.createTempFile(Graphviz.class.toString() + "_", ".dot",
        new File(tempDir));
    dotExporter.exportGraph(graph, dotFile);
    generateImageFromDotFile(dotFile.getAbsolutePath(), imageFilePath,
        DEFAULT_GRAPHVIZ_OUTPUT_TYPE, DEFAULT_GRAPHVIZ_REPRESENTATION_TYPE,
        DEFAULT_GRAPHVIZ_DPI);
    dotFile.delete();
  }

  private void generateImageFromDotFile(String dotFilePath,
      String imageFilePath, String fileType, String representationType, int dpi)
          throws InterruptedException, IOException {
    File imageFile = new File(imageFilePath);
    Runtime runtime = Runtime.getRuntime();
    String[] args = { graphvizPath, "-T" + fileType, "-K" + representationType,
        "-Gdpi=" + dpi, dotFilePath, "-o", imageFile.getAbsolutePath() };
    Process graphvizProcess = runtime.exec(args);
    graphvizProcess.waitFor();
  }

  private class UserIdProvider implements ComponentNameProvider<User> {

    @Override
    public String getName(User arg0) {
      return String.valueOf(arg0.getUserId());
    }

  }

  private class UserFullNameProvider implements ComponentNameProvider<User> {

    @Override
    public String getName(User arg0) {
      return String.valueOf(arg0.getFullName());
    }

  }

  private class ReplyScoreProvider
      implements ComponentNameProvider<ReplyStats> {

    @Override
    public String getName(ReplyStats arg0) {
      return String.format("%.1f", arg0.getScore());
    }

  }

  private class ReplyStatsEdgeFormatProvider
      implements ComponentAttributeProvider<ReplyStats> {

    private double maxScore;
    private String explicitReplyEdgeColor;
    private String implicitReplyEdgeColor;

    public ReplyStatsEdgeFormatProvider(double maxScore,
        String explicitReplyEdgeColor, String implicitReplyEdgeColor) {
      this.maxScore = maxScore;
      this.explicitReplyEdgeColor = explicitReplyEdgeColor;
      this.implicitReplyEdgeColor = implicitReplyEdgeColor;
    }

    @Override
    public Map<String, Attribute> getComponentAttributes(ReplyStats arg0) {
      Map<String, Attribute> attributes = new HashMap<>();
      double lineWidth = DEFAULT_NETWORK_EDGES_MIN_WIDTH
          + (DEFAULT_NETWORK_EDGES_MAX_WIDTH - DEFAULT_NETWORK_EDGES_MIN_WIDTH)
              * arg0.getScore() / maxScore;
      String styleValue = "setlinewidth(" + lineWidth + ")";
      String colorValue = arg0.isExplicitReply() ? explicitReplyEdgeColor
          : implicitReplyEdgeColor;
      attributes.put("style",
          new DefaultAttribute<String>(styleValue, AttributeType.STRING));
      attributes.put("color",
          new DefaultAttribute<String>(colorValue, AttributeType.STRING));
      return attributes;
    }

  }

  private class UserVertexFormatProvider
      implements ComponentAttributeProvider<User> {

    @Override
    public Map<String, Attribute> getComponentAttributes(User arg0) {
      Map<String, Attribute> attributes = new HashMap<>();
      attributes.put("fontname", new DefaultAttribute<String>(
          DEFAULT_GRAPHVIZ_FONT_NAME, AttributeType.STRING));
      return attributes;
    }

  }

}
