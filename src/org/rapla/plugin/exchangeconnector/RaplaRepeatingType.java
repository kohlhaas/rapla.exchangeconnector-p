/**
 * 
 */
package org.rapla.plugin.exchangeconnector;

import org.rapla.entities.domain.RepeatingType;

/**
 * Transformation of the original Rapla class RepeatingType into a enumeration,
 * thus making the types switchable and more useful.
 * 
 * @author lutz
 * @see {@link RepeatingType}
 */
public enum RaplaRepeatingType {
		DAILY(RepeatingType.DAILY),
		WEEKLY(RepeatingType.WEEKLY),
		MONTHLY(RepeatingType.MONTHLY),
		YEARLY(RepeatingType.YEARLY);
		
		
		private RepeatingType repeatingType;
		
		/**
		 * Private constructor to fill the enumeration's {@link RepeatingType}-field
		 * 
		 * @param repeatingType : {@link RepeatingType}
		 */
		private RaplaRepeatingType(RepeatingType repeatingType){
			setRepeatingType(repeatingType);
		}
		
		
		/**
		 * Convert the type to a message {@link String}
		 * 
		 * @see java.lang.Enum#toString()
		 */
		public String toString(){
			return "RepeatingType: "+getRepeatingType().toString();
		}
		
		
		/**
		 * Static method to receive an RaplaRepeatingType object from a particular {@link RepeatingType}
		 * 
		 * @param type : {@link RepeatingType}
		 * @return RepeatingTypeENUM
		 */
		public static RaplaRepeatingType getRaplaRepeatingType(RepeatingType type) {
			RaplaRepeatingType returnType = null;
			// iterate over all existing RepeatingTypes
			for (RaplaRepeatingType repeatingTypeObj : RaplaRepeatingType.values()) {
				// given RepeatingType equals an existing RepeatingType
				if (repeatingTypeObj.getRepeatingType().equals(type))
					// return the found RepeatingTypeEnum
					returnType = repeatingTypeObj;
			}		
			return returnType;
		}

		/**
		 * Get the classic {@link RepeatingType} of the current enum-object
		 * 
		 * @return the repeatingType : {@link RepeatingType}
		 */
		public RepeatingType getRepeatingType() {
			return repeatingType;
		}

		/**
		 * Set the {@link RepeatingType}
		 * 
		 * @param repeatingType : {@link RepeatingType} the repeatingType to set
		 */
		private void setRepeatingType(RepeatingType repeatingType) {
			this.repeatingType = repeatingType;
		}
}
