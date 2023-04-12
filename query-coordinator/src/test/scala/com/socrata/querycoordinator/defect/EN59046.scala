package com.socrata.querycoordinator.defect

import com.socrata.querycoordinator.QueryRewritingTestUtility.AssertRewriteDefault
import com.socrata.soql.types.{SoQLBoolean, SoQLFloatingTimestamp, SoQLNumber, SoQLText}
import org.scalatest.FunSuite

//https://socrata.atlassian.net/browse/EN-59046
//I think th problem here is that this scenario should NOT trigger rewriting?
//'ei_auto_rollup_28_2022-03-10_21-00-35' is the rollup currently hit by both query variants
class EN59046 extends FunSuite{

  test("example one, sum") {
    AssertRewriteDefault(
      Map(
        "_" -> Map(
          "rowkey_column" -> (SoQLNumber.t, "rowkey"),
          "efspname_column" -> (SoQLText.t, "efspname"),
          "county_column" -> (SoQLText.t, "county"),
          "office_column" -> (SoQLText.t, "office"),
          "orgchartname_column" -> (SoQLText.t, "orgchartname"),
          "nodeid_column" -> (SoQLNumber.t, "nodeid"),
          "casenumber_column" -> (SoQLText.t, "casenumber"),
          "envelopeid_column" -> (SoQLNumber.t, "envelopeid"),
          "isinitial_column" -> (SoQLBoolean.t, "isinitial"),
          "filingid_column" -> (SoQLNumber.t, "filingid"),
          "filingtype_column" -> (SoQLText.t, "filingtype"),
          "submitted_column" -> (SoQLFloatingTimestamp.t, "submitted"),
          "submittedday_column" -> (SoQLText.t, "submittedday"),
          "filingstate_column" -> (SoQLText.t, "filingstate"),
          "casecategorycode_column" -> (SoQLText.t, "casecategorycode"),
          "casetypecode_column" -> (SoQLText.t, "casetypecode"),
          "filingcode_column" -> (SoQLText.t, "filingcode"),
          "isindividualfirm_column" -> (SoQLBoolean.t, "isindividualfirm"),
          "username_column" -> (SoQLText.t, "username"),
          "firmid_column" -> (SoQLNumber.t, "firmid"),
          "firmname_column" -> (SoQLText.t, "firmname"),
          "rejectionreason_column" -> (SoQLText.t, "rejectionreason"),
          "numberofdocuments_column" -> (SoQLNumber.t, "numberofdocuments"),
          "underreview_column" -> (SoQLFloatingTimestamp.t, "underreview"),
          "reviewed_column" -> (SoQLFloatingTimestamp.t, "reviewed"),
          "primaryamount_column" -> (SoQLNumber.t, "primaryamount"),
          "secondaryamount_column" -> (SoQLNumber.t, "secondaryamount"),
          "systempaymentaccounttype_column" -> (SoQLText.t, "systempaymentaccounttype"),
          "adjustedsubmitdate_column" -> (SoQLFloatingTimestamp.t, "adjustedsubmitdate"),
          "clerkresponsetime_column" -> (SoQLNumber.t, "clerkresponsetime"),
          "datefilter_column" -> (SoQLNumber.t, "datefilter"),


        )
      ),
      Map(
        //ei_auto_rollup_28_2022-03-10_21-00-35
        "one" -> "SELECT `casetypecode`, `filingstate`, `efspname`, date_trunc_ymd(`adjustedsubmitdate`), date_trunc_ym(`adjustedsubmitdate`), date_trunc_y(`adjustedsubmitdate`), min(`adjustedsubmitdate`), count(`filingid`), count(`casecategorycode`), count(`casetypecode`), count(`county`), count(`filingstate`), count(`efspname`), count(`rejectionreason`), count(`orgchartname`), count(`office`), count(`filingcode`), count(*) GROUP BY `casetypecode`, `filingstate`, `efspname`, date_trunc_ymd(`adjustedsubmitdate`), date_trunc_ym(`adjustedsubmitdate`), date_trunc_y(`adjustedsubmitdate`)"
      ),
      "SELECT filingid, 1 as adder |> select sum(adder)",
      "SELECT filingid, 1 as adder |> select sum(adder)",
      None
    )
  }

  test("example one, count") {
    AssertRewriteDefault(
      Map(
        "_" -> Map(
          "rowkey_column" -> (SoQLNumber.t, "rowkey"),
          "efspname_column" -> (SoQLText.t, "efspname"),
          "county_column" -> (SoQLText.t, "county"),
          "office_column" -> (SoQLText.t, "office"),
          "orgchartname_column" -> (SoQLText.t, "orgchartname"),
          "nodeid_column" -> (SoQLNumber.t, "nodeid"),
          "casenumber_column" -> (SoQLText.t, "casenumber"),
          "envelopeid_column" -> (SoQLNumber.t, "envelopeid"),
          "isinitial_column" -> (SoQLBoolean.t, "isinitial"),
          "filingid_column" -> (SoQLNumber.t, "filingid"),
          "filingtype_column" -> (SoQLText.t, "filingtype"),
          "submitted_column" -> (SoQLFloatingTimestamp.t, "submitted"),
          "submittedday_column" -> (SoQLText.t, "submittedday"),
          "filingstate_column" -> (SoQLText.t, "filingstate"),
          "casecategorycode_column" -> (SoQLText.t, "casecategorycode"),
          "casetypecode_column" -> (SoQLText.t, "casetypecode"),
          "filingcode_column" -> (SoQLText.t, "filingcode"),
          "isindividualfirm_column" -> (SoQLBoolean.t, "isindividualfirm"),
          "username_column" -> (SoQLText.t, "username"),
          "firmid_column" -> (SoQLNumber.t, "firmid"),
          "firmname_column" -> (SoQLText.t, "firmname"),
          "rejectionreason_column" -> (SoQLText.t, "rejectionreason"),
          "numberofdocuments_column" -> (SoQLNumber.t, "numberofdocuments"),
          "underreview_column" -> (SoQLFloatingTimestamp.t, "underreview"),
          "reviewed_column" -> (SoQLFloatingTimestamp.t, "reviewed"),
          "primaryamount_column" -> (SoQLNumber.t, "primaryamount"),
          "secondaryamount_column" -> (SoQLNumber.t, "secondaryamount"),
          "systempaymentaccounttype_column" -> (SoQLText.t, "systempaymentaccounttype"),
          "adjustedsubmitdate_column" -> (SoQLFloatingTimestamp.t, "adjustedsubmitdate"),
          "clerkresponsetime_column" -> (SoQLNumber.t, "clerkresponsetime"),
          "datefilter_column" -> (SoQLNumber.t, "datefilter"),


        )
      ),
      Map(
        //ei_auto_rollup_28_2022-03-10_21-00-35
        "one" -> "SELECT `casetypecode`, `filingstate`, `efspname`, date_trunc_ymd(`adjustedsubmitdate`), date_trunc_ym(`adjustedsubmitdate`), date_trunc_y(`adjustedsubmitdate`), min(`adjustedsubmitdate`), count(`filingid`), count(`casecategorycode`), count(`casetypecode`), count(`county`), count(`filingstate`), count(`efspname`), count(`rejectionreason`), count(`orgchartname`), count(`office`), count(`filingcode`), count(*) GROUP BY `casetypecode`, `filingstate`, `efspname`, date_trunc_ymd(`adjustedsubmitdate`), date_trunc_ym(`adjustedsubmitdate`), date_trunc_y(`adjustedsubmitdate`)"
      ),
      "SELECT filingid, 1 as adder |> select count(adder)",
      "SELECT filingid, 1 as adder |> select count(adder)",
      None
    )
  }
}
