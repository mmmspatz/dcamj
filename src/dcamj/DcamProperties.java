package dcamj;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bridj.BridJ;
import org.bridj.FlagSet;
import org.bridj.IntValuedEnum;
import org.bridj.Pointer;
import org.bridj.Pointer.StringType;

import dcamapi.DCAM_PROPERTYATTR;
import dcamapi.DcamapiLibrary;
import dcamapi.DcamapiLibrary.DCAMERR;
import dcamapi.DcamapiLibrary.DCAMIDPROP;
import dcamapi.DcamapiLibrary.DCAMPROPATTRIBUTE;
import dcamapi.DcamapiLibrary.DCAMPROPUNIT;

public class DcamProperties extends DcamBase
{
	private DcamDevice mDcamDevice;

	private HashMap<String, DcamProperty> mPropertyMap = new HashMap<String, DcamProperty>();

	public DcamProperties(DcamDevice pDcamDevice)
	{
		mDcamDevice = pDcamDevice;
		updatePropertyList();
	}

	public final boolean updatePropertyList()
	{
		boolean lSuccess = true;

		@SuppressWarnings(
		{ "unchecked", "unchecked" })
		Pointer<IntValuedEnum<DcamapiLibrary.DCAMIDPROP>> lPointerToPropertyId = (Pointer) Pointer.allocateCLong();

		while (DcamLibrary.hasSucceeded(DcamapiLibrary.dcampropGetnextid(	mDcamDevice.getHDCAMPointer(),
																																			lPointerToPropertyId,
																																			DcamapiLibrary.DCAMPROPOPTION.DCAMPROP_OPTION_SUPPORT.value)))
		{

			DcamProperty lDcamProperty = new DcamProperty();

			lDcamProperty.id = lPointerToPropertyId.getCLong();

			{
				Pointer<Byte> lNameBytes = Pointer.allocateBytes(64);
				IntValuedEnum<DCAMERR> lError = DcamapiLibrary.dcampropGetname(	mDcamDevice.getHDCAMPointer(),
																																				lPointerToPropertyId.getCLong(),
																																				lNameBytes,
																																				64L);
				final boolean lSuccessGetName = addErrorToListAndCheckHasSucceeded(lError);
				lSuccess &= lSuccessGetName;
				if (!lSuccessGetName)
					break;

				lDcamProperty.name = lNameBytes.getString(StringType.C);
			}

			{
				DCAM_PROPERTYATTR lDCAM_PROPERTYATTR = new DCAM_PROPERTYATTR();
				lDCAM_PROPERTYATTR.cbSize(BridJ.sizeOf(DCAM_PROPERTYATTR.class));
				lDCAM_PROPERTYATTR.iProp(lPointerToPropertyId.getCLong());

				@SuppressWarnings("unchecked")
				FlagSet<DCAMERR> lError = (FlagSet<DCAMERR>) DcamapiLibrary.dcampropGetattr(mDcamDevice.getHDCAMPointer(),
																																										Pointer.pointerTo(lDCAM_PROPERTYATTR));
				final boolean lSuccessGetAttribute = true; // always works...
				lSuccess &= lSuccessGetAttribute;
				// System.out.format("name: %s error: %s \n", lDcamProperty.name,
				// lError.toString());

				if (lSuccessGetAttribute)
				{
					FlagSet<DCAMPROPATTRIBUTE> lFlagSetForAttribute = FlagSet.fromValue(lDCAM_PROPERTYATTR.attribute()
																																																.value(),
																																							DCAMPROPATTRIBUTE.class);

					FlagSet<DCAMPROPUNIT> lFlagSetForUnit = FlagSet.fromValue(lDCAM_PROPERTYATTR.iUnit()
																																											.value(),
																																		DCAMPROPUNIT.class);

					lDcamProperty.attribute = lFlagSetForAttribute;
					lDcamProperty.writable = lFlagSetForAttribute.has(DCAMPROPATTRIBUTE.DCAMPROP_ATTR_WRITABLE);

					if (lFlagSetForAttribute.has(DCAMPROPATTRIBUTE.DCAMPROP_TYPE_LONG))
					{
						lDcamProperty.mode = "long";
					}
					else if (lFlagSetForAttribute.has(DCAMPROPATTRIBUTE.DCAMPROP_TYPE_REAL))
					{
						lDcamProperty.mode = "real";
					}
					else if (lFlagSetForAttribute.has(DCAMPROPATTRIBUTE.DCAMPROP_TYPE_MODE))
					{
						lDcamProperty.mode = "mode";
					}

					lDcamProperty.writable = lFlagSetForAttribute.has(DCAMPROPATTRIBUTE.DCAMPROP_ATTR_WRITABLE);
					lDcamProperty.readable = lFlagSetForAttribute.has(DCAMPROPATTRIBUTE.DCAMPROP_ATTR_READABLE);

					Iterator<DCAMPROPUNIT> lIterator = lFlagSetForUnit.iterator();
					if (lIterator.hasNext())
						lDcamProperty.unit = lIterator.next();
					else
						lDcamProperty.unit = null;

					lDcamProperty.valuemin = lDCAM_PROPERTYATTR.valuemin();
					lDcamProperty.valuemax = lDCAM_PROPERTYATTR.valuemax();
					lDcamProperty.valuestep = lDCAM_PROPERTYATTR.valuestep();
					lDcamProperty.valuedefault = lDCAM_PROPERTYATTR.valuedefault();
				}
			}

			mPropertyMap.put(lDcamProperty.name, lDcamProperty);

		}

		return lSuccess;
	}

	public final Collection<DcamProperty> getPropertyList()
	{
		return mPropertyMap.values();
	}

	public final DcamProperty getProperty(final String pPropertyName)
	{
		return mPropertyMap.get(pPropertyName);
	}

	public final double getPropertyDefaultValue(final String pPropertyName)
	{
		return getProperty(pPropertyName).valuedefault;
	}

	public final double getPropertyMinValue(final String pPropertyName)
	{
		return getProperty(pPropertyName).valuemin;
	}

	public final double getPropertyMaxValue(final String pPropertyName)
	{
		return getProperty(pPropertyName).valuemax;
	}

	public final double getPropertySteps(final String pPropertyName)
	{
		return getProperty(pPropertyName).valuestep;
	}

	public final boolean isPropertyWritable(final String pPropertyName)
	{
		return getProperty(pPropertyName).writable;
	}

	public final boolean isPropertyReadable(final String pPropertyName)
	{
		return getProperty(pPropertyName).readable;
	}

	public final boolean isPropertyReal(final String pPropertyName)
	{
		return getProperty(pPropertyName).mode == "real";
	}

	public final boolean isPropertyLong(final String pPropertyName)
	{
		return getProperty(pPropertyName).mode == "long";
	}

	public double getPropertyValue(DCAMIDPROP pDCAMIDPROP)
	{
		Pointer<Double> lPointerToDouble = Pointer.allocateDouble();

		IntValuedEnum<DCAMERR> lError = DcamapiLibrary.dcampropGetvalue(mDcamDevice.getHDCAMPointer(),
																																		pDCAMIDPROP.value,
																																		lPointerToDouble);
		final boolean lSuccess = addErrorToListAndCheckHasSucceeded(lError);

		if (!lSuccess)
			return Double.NaN;

		final double lValue = lPointerToDouble.getDouble();

		return lValue;
	}

	public final double getPropertyValue(final String pPropertyName)
	{
		DcamProperty lProperty = getProperty(pPropertyName);

		Pointer<Double> lPointerToDouble = Pointer.allocateDouble();

		IntValuedEnum<DCAMERR> lError = DcamapiLibrary.dcampropGetvalue(mDcamDevice.getHDCAMPointer(),
																																		lProperty.id,
																																		lPointerToDouble);
		final boolean lSuccess = addErrorToListAndCheckHasSucceeded(lError);

		if (!lSuccess)
			return Double.NaN;

		final double lValue = lPointerToDouble.getDouble();

		return lValue;
	}

	public final boolean setPropertyValue(final String pPropertyName,
																				final double pValue)
	{
		DcamProperty lProperty = getProperty(pPropertyName);

		IntValuedEnum<DCAMERR> lError = DcamapiLibrary.dcampropSetvalue(mDcamDevice.getHDCAMPointer(),
																																		lProperty.id,
																																		pValue);
		final boolean lSuccess = addErrorToListAndCheckHasSucceeded(lError);

		return lSuccess;
	}

	public final boolean setPropertyValue(final DCAMIDPROP pDCAMIDPROP,
																				final double pValue)
	{

		IntValuedEnum<DCAMERR> lError = DcamapiLibrary.dcampropSetvalue(mDcamDevice.getHDCAMPointer(),
																																		pDCAMIDPROP.value,
																																		pValue);
		final boolean lSuccess = addErrorToListAndCheckHasSucceeded(lError);

		return lSuccess;
	}
	
	private double setAndGetPropertyValue(DCAMIDPROP pDCAMIDPROP,
																				double pValue)
	{
		Pointer<Double> lPointerToDouble = Pointer.allocateDouble();
		lPointerToDouble.set(pValue);
		
		IntValuedEnum<DCAMERR> lError = DcamapiLibrary.dcampropSetgetvalue(mDcamDevice.getHDCAMPointer(),
																																		pDCAMIDPROP.value,
																																		lPointerToDouble,
																																		0);
		final boolean lSuccess = addErrorToListAndCheckHasSucceeded(lError);
		
		if (!lSuccess)
			return Double.NaN;

		final double lValue = lPointerToDouble.getDouble();

		return lValue;
	}

	public void listAllProperties()
	{
		for (Entry<String, DcamProperty> lEntry : mPropertyMap.entrySet())
		{
			final String lName = lEntry.getKey();
			final DcamProperty lDcamProperty = lEntry.getValue();
			System.out.format("property: '%s' \n%s \n",
												lName,
												lDcamProperty.toString());
		}
	}

	public double getExposure()
	{
		return getPropertyValue(DCAMIDPROP.DCAM_IDPROP_EXPOSURETIME);
	}

	public void setExposure(double pExposure)
	{
		setPropertyValue(DCAMIDPROP.DCAM_IDPROP_EXPOSURETIME, pExposure);
	}

	public double setAndGetExposure(double pExposure)
	{
		return setAndGetPropertyValue(DCAMIDPROP.DCAM_IDPROP_EXPOSURETIME, pExposure);
	}

	
	public void setCenteredROI(final int pWidth, final int pHeight)
	{
		final int hpos = 1024-pWidth/2;
		final int vpos = 1024-pHeight/2;
		setPropertyValue(DCAMIDPROP.DCAM_IDPROP_SUBARRAYHPOS, hpos);
		setPropertyValue(DCAMIDPROP.DCAM_IDPROP_SUBARRAYVPOS, vpos);
		setPropertyValue(DCAMIDPROP.DCAM_IDPROP_SUBARRAYHSIZE, pWidth);
		setPropertyValue(DCAMIDPROP.DCAM_IDPROP_SUBARRAYVSIZE, pHeight);
		
		setPropertyValue(DCAMIDPROP.DCAM_IDPROP_SUBARRAYMODE, 2);
	}


}