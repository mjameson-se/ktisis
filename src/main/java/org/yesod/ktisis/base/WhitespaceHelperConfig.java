/**
 * File comment for WhitespaceHelperConfig
 * With multiple lines an such WhitespaceHelperConfig again
 * And testing functions like toUpper: WHITESPACEHELPERCONFIG
 * And substr 1: i
 * And substr 0: itespaceHelperConfig
 * */
package org.yesod.ktisis.base;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/** Helps to format code with whitespace and line wrapping */
public class WhitespaceHelperConfig
{
  private final String preJoin;
  private final String postJoin;
  private final Boolean extraNewlinesIfWrapped;
  private final Integer lineLength;
  private final Integer wrappedIndent;

  public WhitespaceHelperConfig(String preJoin, String postJoin, Boolean extraNewlinesIfWrapped, Integer lineLength, Integer wrappedIndent)
  {

    Preconditions.checkNotNull(preJoin, "preJoin (String) is a required field");
    this.preJoin = preJoin;
    Preconditions.checkNotNull(postJoin, "postJoin (String) is a required field");
    this.postJoin = postJoin;
    Preconditions.checkNotNull(extraNewlinesIfWrapped, "extraNewlinesIfWrapped (Boolean) is a required field");
    this.extraNewlinesIfWrapped = extraNewlinesIfWrapped;
    Preconditions.checkNotNull(lineLength, "lineLength (Integer) is a required field");
    this.lineLength = lineLength;
    Preconditions.checkNotNull(wrappedIndent, "wrappedIndent (Integer) is a required field");
    this.wrappedIndent = wrappedIndent;
  }

  public String getPreJoin()
  {
    return preJoin;
  }

  public String getPostJoin()
  {
    return postJoin;
  }

  public Boolean isExtraNewlinesIfWrapped()
  {
    return extraNewlinesIfWrapped;
  }

  public Integer getLineLength()
  {
    return lineLength;
  }

  public Integer getWrappedIndent()
  {
    return wrappedIndent;
  }

  public static class Builder
  {
    private String preJoin = "";
    private String postJoin = "";
    private Boolean extraNewlinesIfWrapped = Boolean.FALSE;
    private Integer lineLength = 80;
    private Integer wrappedIndent;

    public Builder()
    {
    }

    public Builder(WhitespaceHelperConfig original)
    {
      this.preJoin = original.getPreJoin();
      this.postJoin = original.getPostJoin();
      this.extraNewlinesIfWrapped = original.isExtraNewlinesIfWrapped();
      this.lineLength = original.getLineLength();
      this.wrappedIndent = original.getWrappedIndent();
    }

    public Builder preJoin(String preJoin)
    {
      this.preJoin = preJoin;
      return this;
    }

    public Builder postJoin(String postJoin)
    {
      this.postJoin = postJoin;
      return this;
    }

    public Builder extraNewlinesIfWrapped(Boolean extraNewlinesIfWrapped)
    {
      this.extraNewlinesIfWrapped = extraNewlinesIfWrapped;
      return this;
    }

    public Builder lineLength(Integer lineLength)
    {
      this.lineLength = lineLength;
      return this;
    }

    public Builder wrappedIndent(Integer wrappedIndent)
    {
      this.wrappedIndent = wrappedIndent;
      return this;
    }

    public WhitespaceHelperConfig build()
    {
      return new WhitespaceHelperConfig(preJoin, postJoin, extraNewlinesIfWrapped, lineLength, wrappedIndent);
    }
  }

  @Override
  public int hashCode()
  {
    return Objects.hashCode(preJoin, postJoin, extraNewlinesIfWrapped, lineLength, wrappedIndent);
  }

  @Override
  public boolean equals(Object other)
  {
    if (this == other)
    {
      return true;
    }

    if (!(other instanceof WhitespaceHelperConfig))
    {
      return false;
    }

    WhitespaceHelperConfig that = (WhitespaceHelperConfig) other;

    return Objects.equal(this.preJoin, that.preJoin)
           && Objects.equal(this.postJoin, that.postJoin)
           && Objects.equal(this.extraNewlinesIfWrapped, that.extraNewlinesIfWrapped)
           && Objects.equal(this.lineLength, that.lineLength)
           && Objects.equal(this.wrappedIndent, that.wrappedIndent);
  }

  @Override
  public String toString()
  {
    return MoreObjects.toStringHelper(WhitespaceHelperConfig.class)
                      .add("preJoin", preJoin)
                      .add("postJoin", postJoin)
                      .add("extraNewlinesIfWrapped", extraNewlinesIfWrapped)
                      .add("lineLength", lineLength)
                      .add("wrappedIndent", wrappedIndent)
                      .toString();
  }

  public static WhitespaceHelperConfig lineJoiner(int indent)
  {
    return new WhitespaceHelperConfig.Builder().lineLength(0).wrappedIndent(indent).build();
  }
}
